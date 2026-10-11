package com.campus.volunteer.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.volunteer.dto.Result;
import com.campus.volunteer.entity.Activity;
import com.campus.volunteer.entity.Enrollment;
import com.campus.volunteer.mapper.EnrollmentMapper;
import com.campus.volunteer.service.IActivityService;
import com.campus.volunteer.service.IEnrollmentService;
import com.campus.volunteer.utils.RedisIdWorker;
import com.campus.volunteer.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.campus.volunteer.utils.RedisConstants.ACTIVITY_SIGN_KEY;
import static com.campus.volunteer.utils.RedisConstants.ACTIVITY_SIGN_KEY;
import static com.campus.volunteer.utils.RedisConstants.CACHE_ACTIVITY_KEY;
import static com.campus.volunteer.utils.RedisConstants.ENROLL_STOCK_KEY;
import static com.campus.volunteer.utils.RedisConstants.ENROLL_USER_KEY;

/**
 * 报名服务实现——由课程"秒杀下单"改造而来，相对原代码的关键升级：
 *
 * 1. 修复原bug：原代码 setSql("stock = stocl - 1") 拼写错误（运行时SQL异常）
 * 2. 修复原bug：原代码用 beginTime 判断"秒杀已结束"（应为 endTime）
 * 3. 补齐"一人一单"：Lua集合校验 + 数据库唯一索引，双层防护
 * 4. Lua原子化：校验+扣减合并执行，消除并发下的超卖窗口
 * 5. 失败补偿：数据库落库失败时回补Redis预扣的名额
 * 6. 冷启动兜底：Redis数据缺失时从数据库重建名额与报名集合
 *
 * 说明：当前为同步落库版（校园场景并发量足够）；课程后期的"MQ异步下单"
 * 属高并发优化，列为二期。
 */
@Slf4j
@Service
public class EnrollmentServiceImpl extends ServiceImpl<EnrollmentMapper, Enrollment> implements IEnrollmentService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IActivityService activityService;

    @Resource
    private RedisIdWorker redisIdWorker;

    /** 编程式事务：避免同类内部调用@Transactional失效的坑 */
    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 抢报名 Lua 脚本（课程 seckill.lua 校园版）
     */
    private static final DefaultRedisScript<Long> ENROLL_SCRIPT;
    static {
        ENROLL_SCRIPT = new DefaultRedisScript<>();
        ENROLL_SCRIPT.setLocation(new ClassPathResource("lua/enroll.lua"));
        ENROLL_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result enroll(Long activityId) {
        Long userId = UserHolder.getUser().getId();
        // 1.查询活动：直接查库，报名判断需要最新名额数据（不走缓存）
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            return Result.fail("活动不存在");
        }
        // 2.校验活动状态与报名窗口
        if (activity.getStatus() == null || activity.getStatus() != 1) {
            return Result.fail("活动当前不可报名");
        }
        LocalDateTime now = LocalDateTime.now();
        if (activity.getEnrollStartTime() != null && now.isBefore(activity.getEnrollStartTime())) {
            return Result.fail("报名尚未开始");
        }
        if (activity.getEnrollEndTime() != null && now.isAfter(activity.getEnrollEndTime())) {
            return Result.fail("报名已截止");
        }
        boolean limited = activity.getTotalQuota() != null && activity.getTotalQuota() > 0;
        if (limited) {
            // 3.限量活动：先做冷启动兜底，再执行Lua原子校验与预扣
            ensureRedisState(activity);
            Long r = stringRedisTemplate.execute(
                    ENROLL_SCRIPT,
                    Arrays.asList(ENROLL_STOCK_KEY + activityId, ENROLL_USER_KEY + activityId),
                    activityId.toString(), userId.toString());
            if (r == null || r == 3) {
                return Result.fail("名额数据异常，请稍后重试");
            }
            if (r == 1) {
                return Result.fail("名额已满");
            }
            if (r == 2) {
                return Result.fail("请勿重复报名");
            }
        } else {
            // 不限额活动：仅做报名集合校验（并发由数据库唯一索引兜底）
            Boolean isMember = stringRedisTemplate.opsForSet()
                    .isMember(ENROLL_USER_KEY + activityId, userId.toString());
            if (Boolean.TRUE.equals(isMember)) {
                return Result.fail("请勿重复报名");
            }
        }
        // 4.数据库落库（事务：插入报名记录 + 扣减剩余名额）
        Long enrollId;
        try {
            enrollId = transactionTemplate.execute(status -> {
                // 4.1 插入报名记录：全局唯一单号；uk_user_activity 唯一索引兜底并发重复
                Enrollment enrollment = new Enrollment()
                        .setUserId(userId)
                        .setActivityId(activityId)
                        .setStatus(0)
                        .setEnrollTime(LocalDateTime.now());
                enrollment.setId(redisIdWorker.nextId("enroll"));
                save(enrollment);
                // 4.2 扣减数据库剩余名额（乐观锁语义：left_quota > 0 才扣）
                if (limited) {
                    boolean ok = activityService.update()
                            .setSql("left_quota = left_quota - 1")
                            .eq("id", activityId)
                            .gt("left_quota", 0)
                            .update();
                    if (!ok) {
                        status.setRollbackOnly();
                        return null;
                    }
                }
                return enrollment.getId();
            });
        } catch (DuplicateKeyException e) {
            // 并发下唯一索引兜底命中：回补Redis并提示
            compensate(activity, userId);
            return Result.fail("请勿重复报名");
        }
        if (enrollId == null) {
            // 数据库名额不足（乐观锁失败）：回补Redis预扣的名额
            compensate(activity, userId);
            return Result.fail("名额已满");
        }
        // 5.不限额活动补记报名集合（限量活动Lua内已SADD）
        if (!limited) {
            stringRedisTemplate.opsForSet().add(ENROLL_USER_KEY + activityId, userId.toString());
        }
        // 6.删除活动详情缓存：名额已变化，下次查询从库重建（互斥锁保证重建安全）
        stringRedisTemplate.delete(CACHE_ACTIVITY_KEY + activityId);
        return Result.ok(enrollId);
    }

    @Override
    public Result cancel(Long activityId) {
        Long userId = UserHolder.getUser().getId();
        // 1.查询活动
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            return Result.fail("活动不存在");
        }
        // 2.活动开始后不允许取消
        if (activity.getBeginTime() != null && LocalDateTime.now().isAfter(activity.getBeginTime())) {
            return Result.fail("活动已开始，无法取消报名");
        }
        // 3.查询本人报名记录
        Enrollment enrollment = query().eq("user_id", userId).eq("activity_id", activityId).one();
        if (enrollment == null) {
            return Result.fail("您未报名该活动");
        }
        if (enrollment.getStatus() == null || enrollment.getStatus() != 0) {
            return Result.fail("已签到或已完成的活动不能取消");
        }
        // 4.数据库事务：物理删除报名记录 + 回补剩余名额
        transactionTemplate.executeWithoutResult(status -> {
            removeById(enrollment.getId());
            if (activity.getTotalQuota() != null && activity.getTotalQuota() > 0) {
                activityService.update()
                        .setSql("left_quota = left_quota + 1")
                        .eq("id", activityId)
                        .update();
            }
        });
        // 5.Redis补偿：名额+1、移出报名集合
        //（先库后Redis：若Redis操作失败会造成少量偏差，可由 ensureRedisState 冷启动重建修正）
        if (activity.getTotalQuota() != null && activity.getTotalQuota() > 0) {
            stringRedisTemplate.opsForValue().increment(ENROLL_STOCK_KEY + activityId);
        }
        stringRedisTemplate.opsForSet().remove(ENROLL_USER_KEY + activityId, userId.toString());
        // 6.删除活动详情缓存：名额已回补，下次查询从库重建
        stringRedisTemplate.delete(CACHE_ACTIVITY_KEY + activityId);
        return Result.ok();
    }

    @Override
    public Result myEnrollments() {
        Long userId = UserHolder.getUser().getId();
        // 1.查询本人全部报名记录
        List<Enrollment> list = query().eq("user_id", userId).orderByDesc("enroll_time").list();
        if (list.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 2.批量查询活动并富化信息（避免逐条N+1查询）
        List<Long> activityIds = list.stream().map(Enrollment::getActivityId).distinct().collect(Collectors.toList());
        Map<Long, Activity> activityMap = activityService.listByIds(activityIds).stream()
                .collect(Collectors.toMap(Activity::getId, a -> a));
        for (Enrollment e : list) {
            Activity a = activityMap.get(e.getActivityId());
            if (a != null) {
                e.setActivityTitle(a.getTitle());
                e.setActivityBeginTime(a.getBeginTime());
                e.setActivityAddress(a.getAddress());
            }
        }
        return Result.ok(list);
    }

    @Override
    public Result sign(Long activityId) {
        Long userId = UserHolder.getUser().getId();
        // 1.查询活动
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            return Result.fail("活动不存在");
        }
        // 2.签到窗口：活动开始后至结束前
        LocalDateTime now = LocalDateTime.now();
        if (activity.getBeginTime() != null && now.isBefore(activity.getBeginTime())) {
            return Result.fail("活动尚未开始，无法签到");
        }
        if (activity.getEndTime() != null && now.isAfter(activity.getEndTime())) {
            return Result.fail("活动已结束，无法签到");
        }
        // 3.查询本人报名记录
        Enrollment enrollment = query().eq("user_id", userId).eq("activity_id", activityId).one();
        if (enrollment == null) {
            return Result.fail("您未报名该活动");
        }
        if (enrollment.getStatus() != null && enrollment.getStatus() != 0) {
            return Result.fail("请勿重复签到");
        }
        // 4.BitMap查重（GETBIT，offset=用户ID）
        String signKey = ACTIVITY_SIGN_KEY + activityId;
        Boolean signed = stringRedisTemplate.opsForValue().getBit(signKey, userId);
        if (Boolean.TRUE.equals(signed)) {
            return Result.fail("请勿重复签到");
        }
        // 5.数据库：报名状态置为已签到（先库后缓存）
        updateById(new Enrollment()
                .setId(enrollment.getId())
                .setStatus(1)
                .setSignTime(now));
        // 6.BitMap记录签到（SETBIT），BITCOUNT统计已签到人数
        stringRedisTemplate.opsForValue().setBit(signKey, userId, true);
        // 注：Spring Data Redis 2.3 的 ValueOperations 尚无 bitCount 方法（2.5才加入），
        // 故通过 RedisCallback 走底层连接 API
        Long signedCount = stringRedisTemplate.execute((RedisCallback<Long>) connection ->
                connection.bitCount(signKey.getBytes(StandardCharsets.UTF_8)));
        return Result.ok("签到成功，当前已签到 " + signedCount + " 人，活动结束后由组织方认定志愿时长");
    }

    /**
     * 冷启动兜底：Redis名额key缺失时，从数据库初始化名额与报名集合
     * 场景：Redis重启丢数据、种子数据未经过报名流程
     * 并发安全：setIfAbsent保证名额只初始化一次，SADD天然幂等
     */
    private void ensureRedisState(Activity activity) {
        String stockKey = ENROLL_STOCK_KEY + activity.getId();
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey))) {
            return;
        }
        log.info("活动{}：Redis名额数据缺失，从数据库初始化", activity.getId());
        // 现存报名记录均占用名额（取消的记录已物理删除）
        List<Enrollment> enrollments = query().eq("activity_id", activity.getId()).list();
        stringRedisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(activity.getLeftQuota()));
        if (!enrollments.isEmpty()) {
            String[] userIds = enrollments.stream()
                    .map(e -> e.getUserId().toString()).toArray(String[]::new);
            stringRedisTemplate.opsForSet().add(ENROLL_USER_KEY + activity.getId(), userIds);
        }
    }

    /**
     * Redis回补：数据库落库失败时，恢复Lua预扣的名额并移出报名集合
     */
    private void compensate(Activity activity, Long userId) {
        if (activity.getTotalQuota() != null && activity.getTotalQuota() > 0) {
            stringRedisTemplate.opsForValue().increment(ENROLL_STOCK_KEY + activity.getId());
        }
        stringRedisTemplate.opsForSet().remove(ENROLL_USER_KEY + activity.getId(), userId.toString());
    }
}
