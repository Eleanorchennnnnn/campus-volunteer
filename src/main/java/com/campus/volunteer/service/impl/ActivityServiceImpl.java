package com.campus.volunteer.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.volunteer.dto.Result;
import com.campus.volunteer.entity.Activity;
import com.campus.volunteer.entity.ActivityType;
import com.campus.volunteer.entity.Org;
import com.campus.volunteer.mapper.ActivityMapper;
import com.campus.volunteer.service.IActivityService;
import com.campus.volunteer.service.IActivityTypeService;
import com.campus.volunteer.service.IOrgService;
import com.campus.volunteer.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static com.campus.volunteer.utils.RedisConstants.*;

/**
 * 活动服务实现——本步的核心是"缓存三件套"（源自课程商户缓存章节，迁移到活动业务）：
 *
 * 1. 缓存穿透：查询数据库不存在的活动时，缓存空值 ""（短TTL 2分钟），
 *    避免恶意请求把每个不存在的ID都打到数据库
 *
 * 2. 缓存击穿：热点活动缓存失效瞬间用互斥锁（setIfAbsent）保证只有一个线程重建缓存，
 *    其他线程休眠重试；拿到锁后先做双重检查（DoubleCheck）
 *
 * 3. 缓存雪崩：缓存TTL加随机偏移（30 + 0~9 分钟），避免大量key同时失效
 *
 * 缓存结构：String 类型，值为 Activity 的 JSON 串（课程商户缓存同款方案）
 */
@Slf4j
@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements IActivityService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IOrgService orgService;

    @Resource
    private IActivityTypeService activityTypeService;

    @Override
    public Result queryById(Long id) {
        // 互斥锁版查询：解决缓存击穿
        Activity activity = queryWithMutex(id);
        if (activity == null) {
            return Result.fail("活动不存在");
        }
        // 富化组织名/分类名（不进缓存：组织改名后缓存应立即反映，且查询代价小）
        enrich(activity);
        return Result.ok(activity);
    }

    @Override
    public Result saveActivity(Activity activity) {
        // 1.参数校验
        if (activity.getOrgId() == null || activity.getTypeId() == null
                || StrUtil.isBlank(activity.getTitle())
                || activity.getBeginTime() == null || activity.getEndTime() == null) {
            return Result.fail("组织、分类、标题、起止时间不能为空");
        }
        // 2.组织必须存在
        Org org = orgService.getById(activity.getOrgId());
        if (org == null) {
            return Result.fail("主办方组织不存在");
        }
        // 3.初始化名额与状态：剩余名额=总名额，默认报名中
        if (activity.getTotalQuota() == null) {
            activity.setTotalQuota(0);
        }
        activity.setLeftQuota(activity.getTotalQuota());
        if (activity.getStatus() == null) {
            activity.setStatus(1);
        }
        if (activity.getVolunteerHours() == null) {
            activity.setVolunteerHours(BigDecimal.ZERO);
        }
        // 4.落库（新增的活动尚未被缓存，无需操作缓存）
        save(activity);
        // 5.限量活动：预置Redis报名名额key（课程在新增秒杀券时缓存库存的同款位置）
        if (activity.getTotalQuota() > 0) {
            stringRedisTemplate.opsForValue().set(ENROLL_STOCK_KEY + activity.getId(),
                    String.valueOf(activity.getTotalQuota()));
        }
        return Result.ok(activity.getId());
    }

    @Override
    @Transactional
    public Result updateActivity(Activity activity) {
        Long id = activity.getId();
        if (id == null) {
            return Result.fail("活动id不能为空");
        }
        // 1.更新数据库
        updateById(activity);
        // 2.删除缓存（先更新库再删缓存，下次查询时重建）
        stringRedisTemplate.delete(CACHE_ACTIVITY_KEY + id);
        return Result.ok();
    }

    @Override
    public Result queryByType(Long typeId, Integer current) {
        // 按分类查询"报名中/进行中"的活动，按开始时间正序分页
        Page<Activity> page = query()
                .eq("type_id", typeId)
                .in("status", 1, 2)
                .orderByAsc("begin_time")
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        // 列表同样富化组织名与分类名，便于前端直接展示
        page.getRecords().forEach(this::enrich);
        return Result.ok(page.getRecords());
    }

    /**
     * 互斥锁版缓存查询（缓存击穿防护）
     */
    private Activity queryWithMutex(Long id) {
        String key = CACHE_ACTIVITY_KEY + id;
        // 1.查缓存
        String activityJson = stringRedisTemplate.opsForValue().get(key);
        // 2.命中且为真实数据，直接返回
        if (StrUtil.isNotBlank(activityJson)) {
            log.debug("活动{}：缓存命中", id);
            return JSONUtil.toBean(activityJson, Activity.class);
        }
        // 3.命中但是空字符串（缓存穿透时写入的空值标记），返回null
        if (activityJson != null) {
            log.debug("活动{}：命中空值标记（穿透防护生效）", id);
            return null;
        }
        // 4.缓存未命中，尝试获取互斥锁
        String lockKey = LOCK_ACTIVITY_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 4.1 抢锁失败：说明有线程正在重建缓存，短暂休眠后重试
        if (!isLock) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return queryWithMutex(id);
        }
        // 4.2 抢到锁：重建缓存
        try {
            log.debug("活动{}：缓存未命中，抢到锁开始重建", id);
            // 双重检查：抢锁期间可能已有线程完成重建
            activityJson = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(activityJson)) {
                return JSONUtil.toBean(activityJson, Activity.class);
            }
            if (activityJson != null) {
                return null;
            }
            // 查数据库
            Activity activity = getById(id);
            if (activity == null) {
                // 缓存穿透防护：不存在则缓存空值，短TTL
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                log.debug("活动{}：数据库不存在，已写入空值标记", id);
                return null;
            }
            // 缓存雪崩防护：TTL加随机偏移，避免大量key同时过期
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(activity),
                    CACHE_ACTIVITY_TTL + RandomUtil.randomLong(0, 10), TimeUnit.MINUTES);
            log.debug("活动{}：缓存重建完成", id);
            return activity;
        } finally {
            // 5.释放锁（只释放自己持有的）
            unlock(lockKey);
        }
    }

    /**
     * 尝试获取互斥锁：setIfAbsent = setnx，加TTL防止死锁
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, Thread.currentThread().getId() + "", LOCK_ACTIVITY_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放互斥锁：值比对线程ID，只删自己的锁
     * 注意：GET+比对+DEL三步不是原子操作（极端情况可能误删），
     * 完整方案需要Lua脚本或Redisson，将在报名模块的环节演示
     */
    private void unlock(String key) {
        String threadId = stringRedisTemplate.opsForValue().get(key);
        if ((Thread.currentThread().getId() + "").equals(threadId)) {
            stringRedisTemplate.delete(key);
        }
    }

    /**
     * 富化非表字段：组织名与分类名
     */
    private void enrich(Activity activity) {
        if (activity.getOrgId() != null) {
            Org org = orgService.getById(activity.getOrgId());
            if (org != null) {
                activity.setOrgName(org.getName());
            }
        }
        if (activity.getTypeId() != null) {
            ActivityType type = activityTypeService.getById(activity.getTypeId());
            if (type != null) {
                activity.setTypeName(type.getName());
            }
        }
    }
}
