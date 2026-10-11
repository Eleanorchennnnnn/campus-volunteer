package com.campus.volunteer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.volunteer.dto.Result;
import com.campus.volunteer.entity.Activity;

/**
 * 活动服务接口
 */
public interface IActivityService extends IService<Activity> {

    /**
     * 按ID查询活动详情（带缓存三件套：穿透/击穿/雪崩防护）
     */
    Result queryById(Long id);

    /**
     * 发布活动（组织方/管理员）
     */
    Result saveActivity(Activity activity);

    /**
     * 修改活动（先更新数据库，再删除缓存）
     */
    Result updateActivity(Activity activity);

    /**
     * 按分类分页查询可报名/进行中的活动
     */
    Result queryByType(Long typeId, Integer current);
}
