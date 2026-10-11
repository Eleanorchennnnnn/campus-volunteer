package com.campus.volunteer.controller;

import com.campus.volunteer.dto.Result;
import com.campus.volunteer.entity.Post;
import com.campus.volunteer.service.IPostService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 帖子控制器：活动回顾帖/志愿心得
 * 浏览类接口公开（MvcConfig 排除 /post/* 与 /post/of/activity/**），
 * 发布/点赞等写操作需登录
 */
@RestController
@RequestMapping("/post")
public class PostController {

    @Resource
    private IPostService postService;

    /**
     * 发布帖子（登录）
     */
    @PostMapping
    public Result savePost(@RequestBody Post post) {
        return postService.savePost(post);
    }

    /**
     * 帖子详情（公开，富化作者与点赞状态）
     */
    @GetMapping("/{id}")
    public Result queryById(@PathVariable("id") Long id) {
        return postService.queryById(id);
    }

    /**
     * 点赞/取消点赞（登录，Redis Set 一人一赞）
     */
    @PutMapping("/like/{id}")
    public Result likePost(@PathVariable("id") Long id) {
        return postService.likePost(id);
    }

    /**
     * 热门帖子（公开，按点赞数降序）
     */
    @GetMapping("/hot")
    public Result queryHotPosts(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return postService.queryHotPosts(current);
    }

    /**
     * 我的帖子（登录）
     */
    @GetMapping("/of/me")
    public Result queryMyPosts(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return postService.queryMyPosts(current);
    }

    /**
     * 某活动的全部回顾帖（公开）
     */
    @GetMapping("/of/activity/{activityId}")
    public Result queryPostsOfActivity(@PathVariable("activityId") Long activityId,
                                       @RequestParam(value = "current", defaultValue = "1") Integer current) {
        return postService.queryPostsOfActivity(activityId, current);
    }
}
