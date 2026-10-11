package com.campus.volunteer.controller;

import com.campus.volunteer.dto.Result;
import com.campus.volunteer.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 关注控制器：关注/取关社团组织（需登录）
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;

    /**
     * 关注组织
     */
    @PutMapping("/{orgId}")
    public Result followOrg(@PathVariable("orgId") Long orgId) {
        return followService.followOrg(orgId);
    }

    /**
     * 取消关注组织
     */
    @DeleteMapping("/{orgId}")
    public Result unFollowOrg(@PathVariable("orgId") Long orgId) {
        return followService.unFollowOrg(orgId);
    }

    /**
     * 查询是否已关注该组织
     */
    @GetMapping("/{orgId}")
    public Result isFollowed(@PathVariable("orgId") Long orgId) {
        return followService.isFollowed(orgId);
    }
}
