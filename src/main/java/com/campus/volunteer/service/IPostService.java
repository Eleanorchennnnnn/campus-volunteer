package com.campus.volunteer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.volunteer.dto.Result;
import com.campus.volunteer.entity.Post;

/**
 * 帖子服务接口
 */
public interface IPostService extends IService<Post> {

    /**
     * 发布帖子（活动回顾/志愿心得/普通分享）
     */
    Result savePost(Post post);

    /**
     * 帖子详情（富化作者信息 + 当前用户是否点赞）
     */
    Result queryById(Long id);

    /**
     * 点赞/取消点赞（Redis Set 一人一赞，toggle）
     */
    Result likePost(Long id);

    /**
     * 热门帖子（按点赞数降序分页）
     */
    Result queryHotPosts(Integer current);

    /**
     * 我的帖子
     */
    Result queryMyPosts(Integer current);

    /**
     * 某活动的全部回顾帖
     */
    Result queryPostsOfActivity(Long activityId, Integer current);
}
