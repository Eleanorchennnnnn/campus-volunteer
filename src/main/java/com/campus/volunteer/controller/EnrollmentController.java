package com.campus.volunteer.controller;

import com.campus.volunteer.dto.Result;
import com.campus.volunteer.service.IEnrollmentService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 报名控制器（需登录，任何角色均可报名）
 */
@RestController
@RequestMapping("/enrollment")
public class EnrollmentController {

    @Resource
    private IEnrollmentService enrollmentService;

    /**
     * 报名活动：Lua原子校验预扣 + 事务落库
     */
    @PostMapping("/{activityId}")
    public Result enroll(@PathVariable("activityId") Long activityId) {
        return enrollmentService.enroll(activityId);
    }

    /**
     * 取消报名：活动开始前可取消，名额回补
     */
    @DeleteMapping("/{activityId}")
    public Result cancel(@PathVariable("activityId") Long activityId) {
        return enrollmentService.cancel(activityId);
    }

    /**
     * 我的报名列表
     */
    @GetMapping("/my")
    public Result myEnrollments() {
        return enrollmentService.myEnrollments();
    }

    /**
     * 现场签到：BitMap记录 + 状态0→1
     */
    @PostMapping("/sign/{activityId}")
    public Result sign(@PathVariable("activityId") Long activityId) {
        return enrollmentService.sign(activityId);
    }
}
