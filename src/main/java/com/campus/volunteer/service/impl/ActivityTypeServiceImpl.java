package com.campus.volunteer.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.volunteer.dto.Result;
import com.campus.volunteer.entity.ActivityType;
import com.campus.volunteer.mapper.ActivityTypeMapper;
import com.campus.volunteer.service.IActivityTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.campus.volunteer.utils.RedisConstants.CACHE_ACTIVITY_TYPE_KEY;
import static com.campus.volunteer.utils.RedisConstants.CACHE_ACTIVITY_TYPE_TTL;

/**
 * 活动分类服务实现：分类列表整体作为一个JSON字符串缓存（课程商户类型列表同款方案）
 * 分类数据量小且极少变化，不需要互斥锁与空值防护
 */
@Slf4j
@Service
public class ActivityTypeServiceImpl extends ServiceImpl<ActivityTypeMapper, ActivityType> implements IActivityTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryList() {
        // 1.查缓存
        String json = stringRedisTemplate.opsForValue().get(CACHE_ACTIVITY_TYPE_KEY);
        if (StrUtil.isNotBlank(json)) {
            List<ActivityType> types = JSONUtil.toList(json, ActivityType.class);
            return Result.ok(types);
        }
        // 2.未命中，查数据库（按sort排序）
        List<ActivityType> types = query().orderByAsc("sort").list();
        // 3.写入缓存，TTL 1天
        stringRedisTemplate.opsForValue().set(CACHE_ACTIVITY_TYPE_KEY, JSONUtil.toJsonStr(types),
                CACHE_ACTIVITY_TYPE_TTL, TimeUnit.MINUTES);
        return Result.ok(types);
    }
}
