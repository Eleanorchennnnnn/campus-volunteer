package com.campus.volunteer.service.impl;

import com.campus.volunteer.dto.Result;
import com.campus.volunteer.entity.Activity;
import com.campus.volunteer.entity.Enrollment;
import com.campus.volunteer.entity.Org;
import com.campus.volunteer.entity.User;
import com.campus.volunteer.entity.UserInfo;
import com.campus.volunteer.service.IActivityService;
import com.campus.volunteer.service.IEnrollmentService;
import com.campus.volunteer.service.IOrgService;
import com.campus.volunteer.service.IUserInfoService;
import com.campus.volunteer.service.IUserService;
import com.campus.volunteer.service.IVolunteerService;
import com.campus.volunteer.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.campus.volunteer.utils.RedisConstants.CACHE_ACTIVITY_KEY;
import static com.campus.volunteer.utils.RedisConstants.RANK_KEY;

/**
 * 志愿服务实现——平台核心闭环：
 * 现场签到(BitMap) → 组织方结束活动 → 时长认定(累计画像+星级) → ZSet时长榜 → 电子证明
 *
 * 关键顺序说明：ensureRankInit 必须在数据库画像更新【之前】调用，
 * 否则冷启动会加载"已累加"的数值、再ZINCRBY一次造成双倍计数
 */
@Slf4j
@Service
public class VolunteerServiceImpl implements IVolunteerService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IActivityService activityService;

    @Resource
    private IEnrollmentService enrollmentService;

    @Resource
    private IUserInfoService userInfoService;

    @Resource
    private IUserService userService;

    @Resource
    private IOrgService orgService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public Result finishActivity(Long activityId) {
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            return Result.fail("活动不存在");
        }
        if (activity.getStatus() != null && activity.getStatus() == 3) {
            return Result.fail("该活动已结束并完成时长认定");
        }
        if (activity.getBeginTime() != null && LocalDateTime.now().isBefore(activity.getBeginTime())) {
            return Result.fail("活动尚未开始，不能进行时长认定");
        }
        BigDecimal hours = activity.getVolunteerHours() == null ? BigDecimal.ZERO : activity.getVolunteerHours();
        // 0.榜单冷启动初始化（必须在画像更新之前，防止双倍计数）
        ensureRankInit();
        // 1.数据库事务：活动置为已结束，已签到记录置为已完成
        List<Enrollment> signedList = transactionTemplate.execute(status -> {
            activityService.update()
                    .set("status", 3)
                    .eq("id", activityId)
                    .update();
            List<Enrollment> signed = enrollmentService.query()
                    .eq("activity_id", activityId)
                    .eq("status", 1)
                    .list();
            if (!signed.isEmpty()) {
                LocalDateTime finishTime = LocalDateTime.now();
                for (Enrollment e : signed) {
                    e.setStatus(2);
                    e.setFinishTime(finishTime);
                }
                enrollmentService.updateBatchById(signed);
            }
            return signed;
        });
        // 2.逐人累计志愿画像（时长/积分/星级）
        if (signedList != null) {
            for (Enrollment e : signedList) {
                accumulateHours(e.getUserId(), hours);
            }
            // 3.ZSet志愿时长榜累加
            for (Enrollment e : signedList) {
                stringRedisTemplate.opsForZSet()
                        .incrementScore(RANK_KEY, e.getUserId().toString(), hours.doubleValue());
            }
        }
        // 4.删除活动详情缓存（状态已变化，下次查询重建）
        stringRedisTemplate.delete(CACHE_ACTIVITY_KEY + activityId);
        int count = signedList == null ? 0 : signedList.size();
        return Result.ok("时长认定完成：" + count + "人，每人认定" + hours + "小时，共"
                + hours.multiply(BigDecimal.valueOf(count)) + "小时");
    }

    @Override
    public Result rank() {
        ensureRankInit();
        // ZREVRANGEWITHSCORES 取前10（分数降序）
        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(RANK_KEY, 0, 9);
        if (tuples == null || tuples.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 批量查询昵称，避免逐条查库
        List<Long> ids = tuples.stream().map(t -> Long.parseLong(t.getValue())).collect(Collectors.toList());
        Map<Long, User> userMap = userService.listByIds(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        List<Map<String, Object>> board = new ArrayList<>();
        int rankNo = 1;
        for (ZSetOperations.TypedTuple<String> t : tuples) {
            Long userId = Long.parseLong(t.getValue());
            User u = userMap.get(userId);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rankNo++);
            item.put("userId", userId);
            item.put("nickName", u == null ? "志愿者" + userId : u.getNickName());
            item.put("hours", t.getScore());
            item.put("level", calcLevel(BigDecimal.valueOf(t.getScore())));
            board.add(item);
        }
        return Result.ok(board);
    }

    @Override
    public Result myProfile() {
        Long userId = UserHolder.getUser().getId();
        ensureRankInit();
        Double score = stringRedisTemplate.opsForZSet().score(RANK_KEY, userId.toString());
        Long rankNo = stringRedisTemplate.opsForZSet().reverseRank(RANK_KEY, userId.toString());
        UserInfo info = userInfoService.getById(userId);
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("totalHours", info == null || info.getTotalHours() == null ? 0 : info.getTotalHours());
        profile.put("credits", info == null || info.getCredits() == null ? 0 : info.getCredits());
        profile.put("level", info == null || info.getLevel() == null ? 0 : info.getLevel());
        // ZREVRANK从0计，展示用+1
        profile.put("rank", rankNo == null ? null : rankNo + 1);
        profile.put("score", score == null ? 0 : score);
        return Result.ok(profile);
    }

    @Override
    public Result certificate() {
        Long userId = UserHolder.getUser().getId();
        User user = userService.getById(userId);
        UserInfo info = userInfoService.getById(userId);
        // 已完成(status=2)的志愿记录，按完成时间倒序
        List<Enrollment> completed = enrollmentService.query()
                .eq("user_id", userId)
                .eq("status", 2)
                .orderByDesc("finish_time")
                .list();
        Map<String, Object> cert = new LinkedHashMap<>();
        cert.put("name", user == null ? null : user.getNickName());
        cert.put("studentNo", user == null ? null : user.getStudentNo());
        cert.put("totalHours", info == null || info.getTotalHours() == null ? 0 : info.getTotalHours());
        cert.put("credits", info == null || info.getCredits() == null ? 0 : info.getCredits());
        cert.put("level", info == null || info.getLevel() == null ? 0 : info.getLevel());
        cert.put("completedCount", completed.size());
        List<Map<String, Object>> activities = new ArrayList<>();
        if (!completed.isEmpty()) {
            List<Long> activityIds = completed.stream().map(Enrollment::getActivityId).distinct().collect(Collectors.toList());
            Map<Long, Activity> activityMap = activityService.listByIds(activityIds).stream()
                    .collect(Collectors.toMap(Activity::getId, a -> a));
            // 批量查询主办方组织名称
            List<Long> orgIds = activityMap.values().stream()
                    .map(Activity::getOrgId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            Map<Long, Org> orgMap = orgIds.isEmpty() ? Collections.emptyMap()
                    : orgService.listByIds(orgIds).stream().collect(Collectors.toMap(Org::getId, o -> o));
            for (Enrollment e : completed) {
                Activity a = activityMap.get(e.getActivityId());
                if (a == null) {
                    continue;
                }
                Org org = a.getOrgId() == null ? null : orgMap.get(a.getOrgId());
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("title", a.getTitle());
                item.put("orgName", org == null ? null : org.getName());
                item.put("finishTime", e.getFinishTime());
                item.put("hours", a.getVolunteerHours());
                activities.add(item);
            }
        }
        cert.put("activities", activities);
        cert.put("issuedAt", LocalDateTime.now().toString());
        return Result.ok(cert);
    }

    /**
     * 累计志愿画像：总时长+本活动时长、积分(5分/小时)、星级重算
     */
    private void accumulateHours(Long userId, BigDecimal hours) {
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            info = new UserInfo()
                    .setUserId(userId)
                    .setTotalHours(BigDecimal.ZERO)
                    .setCredits(0)
                    .setLevel(0)
                    .setFans(0)
                    .setFollows(0);
        }
        BigDecimal total = info.getTotalHours() == null ? BigDecimal.ZERO : info.getTotalHours();
        BigDecimal newTotal = total.add(hours);
        int addCredits = hours.multiply(BigDecimal.valueOf(5)).setScale(0, RoundingMode.HALF_UP).intValue();
        info.setTotalHours(newTotal);
        info.setCredits((info.getCredits() == null ? 0 : info.getCredits()) + addCredits);
        info.setLevel(calcLevel(newTotal));
        userInfoService.saveOrUpdate(info);
    }

    /**
     * 志愿星级：每满15小时升1星，封顶4星（与种子数据规则一致）
     */
    private int calcLevel(BigDecimal totalHours) {
        if (totalHours == null || totalHours.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        int level = totalHours.divide(BigDecimal.valueOf(15), 0, RoundingMode.DOWN).intValue();
        return Math.min(4, level);
    }

    /**
     * 榜单冷启动：ZSet为空时从 tb_user_info 全量初始化（Redis重启/首次访问兜底）
     * 注意：必须在数据库画像更新之前调用，否则会双倍计数
     */
    private void ensureRankInit() {
        Long size = stringRedisTemplate.opsForZSet().zCard(RANK_KEY);
        if (size != null && size > 0) {
            return;
        }
        log.info("志愿时长榜为空，从数据库初始化");
        List<UserInfo> infos = userInfoService.list();
        for (UserInfo info : infos) {
            if (info.getUserId() != null && info.getTotalHours() != null) {
                stringRedisTemplate.opsForZSet()
                        .add(RANK_KEY, info.getUserId().toString(), info.getTotalHours().doubleValue());
            }
        }
    }
}
