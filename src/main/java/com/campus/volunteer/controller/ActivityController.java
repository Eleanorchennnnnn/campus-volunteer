package com.campus.volunteer.controller;

import com.campus.volunteer.dto.Result;
import com.campus.volunteer.service.IActivityService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 活动控制器：公开浏览（无需登录，与课程商户查询同款定位）
 * 发布/修改活动走 /org/activity（需组织方角色，见 OrgController）
 */
@RestController
@RequestMapping("/activity")
public class ActivityController {

    @Resource
    private IActivityService activityService;

    /**
     * 活动详情（缓存三件套：穿透/击穿/雪崩防护）
     */
    @GetMapping("/{id}")
    public Result queryById(@PathVariable("id") Long id) {
        return activityService.queryById(id);
    }

    /**
     * 按分类分页查询可报名/进行中的活动
     */
    @GetMapping("/of/type")
    public Result queryByType(@RequestParam("typeId") Long typeId,
                              @RequestParam(value = "current", defaultValue = "1") Integer current) {
        return activityService.queryByType(typeId, current);
    }
}
