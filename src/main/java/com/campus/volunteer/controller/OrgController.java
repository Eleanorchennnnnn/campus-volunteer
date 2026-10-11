package com.campus.volunteer.controller;

import com.campus.volunteer.dto.Result;
import com.campus.volunteer.entity.Activity;
import com.campus.volunteer.service.IActivityService;
import com.campus.volunteer.service.IVolunteerService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 组织方接口（/org/** 由 RoleInterceptor 校验角色 >= 1）
 * 发布与修改活动在第四步填充，后续继续扩展组织管理
 */
@RestController
@RequestMapping("/org")
public class OrgController {

    @Resource
    private IActivityService activityService;

    @Resource
    private IVolunteerService volunteerService;

    /**
     * 角色拦截器连通性验证（第三步遗留，可保留作健康检查）
     */
    @GetMapping("/ping")
    public Result ping() {
        return Result.ok("组织方接口连通");
    }

    /**
     * 发布活动（组织方/管理员）
     */
    @PostMapping("/activity")
    public Result saveActivity(@RequestBody Activity activity) {
        return activityService.saveActivity(activity);
    }

    /**
     * 修改活动（组织方/管理员）：先更新数据库，再删缓存
     */
    @PutMapping("/activity")
    public Result updateActivity(@RequestBody Activity activity) {
        return activityService.updateActivity(activity);
    }

    /**
     * 结束活动并认定志愿时长（组织方/管理员）：签到记录置完成，累计画像与时长榜
     */
    @PostMapping("/activity/{id}/finish")
    public Result finishActivity(@PathVariable("id") Long id) {
        return volunteerService.finishActivity(id);
    }
}
