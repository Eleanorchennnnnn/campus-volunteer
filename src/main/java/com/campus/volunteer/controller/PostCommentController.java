package com.campus.volunteer.controller;

import com.campus.volunteer.dto.Result;
import com.campus.volunteer.entity.PostComment;
import com.campus.volunteer.service.IPostCommentService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 帖子评论控制器：评论列表公开，发表/删除需登录
 */
@RestController
@RequestMapping("/post-comment")
public class PostCommentController {

    @Resource
    private IPostCommentService postCommentService;

    /**
     * 发表评论（登录，支持两级评论）
     */
    @PostMapping
    public Result saveComment(@RequestBody PostComment comment) {
        return postCommentService.saveComment(comment);
    }

    /**
     * 查询帖子的全部评论（公开）
     */
    @GetMapping("/of/{postId}")
    public Result queryByPost(@PathVariable("postId") Long postId) {
        return postCommentService.queryByPost(postId);
    }

    /**
     * 删除评论（登录，仅作者本人）
     */
    @DeleteMapping("/{id}")
    public Result deleteComment(@PathVariable("id") Long id) {
        return postCommentService.deleteComment(id);
    }
}
