package com.campus.volunteer.controller;

import com.campus.volunteer.dto.Result;
import com.campus.volunteer.service.IActivityTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 活动分类控制器（公开，JSON列表缓存）
 */
@RestController
@RequestMapping("/activity-type")
public class ActivityTypeController {

    @Resource
    private IActivityTypeService activityTypeService;

    @GetMapping("/list")
    public Result queryList() {
        return activityTypeService.queryList();
    }
}
