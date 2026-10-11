package com.campus.volunteer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.volunteer.dto.Result;
import com.campus.volunteer.entity.Enrollment;

/**
 * 报名服务接口
 */
public interface IEnrollmentService extends IService<Enrollment> {

    /**
     * 抢报名：Lua原子校验预扣 + 事务落库 + 失败补偿
     */
    Result enroll(Long activityId);

    /**
     * 取消报名：事务删除记录回补名额 + Redis补偿
     */
    Result cancel(Long activityId);

    /**
     * 我的报名列表（富化活动信息）
     */
    Result myEnrollments();

    /**
     * 现场签到：BitMap记录 + 报名状态 0→1
     */
    Result sign(Long activityId);
}
