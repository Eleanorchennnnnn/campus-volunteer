package com.campus.volunteer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.volunteer.dto.Result;
import com.campus.volunteer.entity.ActivityType;

/**
 * 活动分类服务接口
 */
public interface IActivityTypeService extends IService<ActivityType> {

    /**
     * 查询全部分类（整体列表以JSON字符串缓存）
     */
    Result queryList();
}
