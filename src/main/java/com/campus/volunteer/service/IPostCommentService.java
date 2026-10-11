package com.campus.volunteer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.volunteer.dto.Result;
import com.campus.volunteer.entity.PostComment;

/**
 * 帖子评论服务接口
 */
public interface IPostCommentService extends IService<PostComment> {

    /**
     * 发表评论（支持两级评论：parentId/answerId）
     */
    Result saveComment(PostComment comment);

    /**
     * 查询帖子的全部评论（富化评论者昵称/头像，按时间正序）
     */
    Result queryByPost(Long postId);

    /**
     * 删除评论（仅作者本人）
     */
    Result deleteComment(Long id);
}
