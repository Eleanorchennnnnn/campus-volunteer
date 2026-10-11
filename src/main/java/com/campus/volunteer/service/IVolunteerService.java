package com.campus.volunteer.service;

import com.campus.volunteer.dto.Result;

/**
 * 志愿服务接口：时长闭环（签到→认定→画像→榜单→证明）
 */
public interface IVolunteerService {

    /**
     * 结束活动并认定志愿时长（组织方/管理员）：
     * 已签到记录置为已完成，累计用户志愿画像，更新ZSet时长榜
     */
    Result finishActivity(Long activityId);

    /**
     * 志愿时长排行榜（ZSet，公开）
     */
    Result rank();

    /**
     * 我的志愿档案（时长/积分/星级/排名）
     */
    Result myProfile();

    /**
     * 志愿服务证明数据（前端渲染打印版）
     */
    Result certificate();
}
