package com.campus.volunteer.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.volunteer.dto.Result;
import com.campus.volunteer.entity.PostComment;
import com.campus.volunteer.entity.User;
import com.campus.volunteer.mapper.PostCommentMapper;
import com.campus.volunteer.service.IPostCommentService;
import com.campus.volunteer.service.IPostService;
import com.campus.volunteer.service.IUserService;
import com.campus.volunteer.utils.UserHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 帖子评论服务实现
 */
@Service
public class PostCommentServiceImpl extends ServiceImpl<PostCommentMapper, PostComment> implements IPostCommentService {

    @Resource
    private IPostService postService;

    @Resource
    private IUserService userService;

    @Override
    public Result saveComment(PostComment comment) {
        Long userId = UserHolder.getUser().getId();
        // 1.参数校验
        if (comment.getPostId() == null) {
            return Result.fail("帖子id不能为空");
        }
        if (StrUtil.isBlank(comment.getContent())) {
            return Result.fail("评论内容不能为空");
        }
        if (comment.getContent().length() > 512) {
            return Result.fail("评论内容过长");
        }
        if (postService.getById(comment.getPostId()) == null) {
            return Result.fail("帖子不存在");
        }
        // 2.父评论校验（两级评论）
        if (comment.getParentId() != null && getById(comment.getParentId()) == null) {
            return Result.fail("回复的评论不存在");
        }
        // 3.落库
        comment.setUserId(userId);
        comment.setLiked(0);
        save(comment);
        return Result.ok(comment.getId());
    }

    @Override
    public Result queryByPost(Long postId) {
        List<PostComment> list = query()
                .eq("post_id", postId)
                .orderByAsc("create_time")
                .list();
        // 批量富化评论者昵称/头像（避免逐条查库）
        if (!list.isEmpty()) {
            List<Long> userIds = list.stream().map(PostComment::getUserId).distinct().collect(Collectors.toList());
            Map<Long, User> userMap = userService.listByIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
            for (PostComment c : list) {
                User u = userMap.get(c.getUserId());
                if (u != null) {
                    c.setNickName(u.getNickName());
                    c.setIcon(u.getIcon());
                }
            }
        }
        return Result.ok(list);
    }

    @Override
    public Result deleteComment(Long id) {
        PostComment comment = getById(id);
        if (comment == null) {
            return Result.fail("评论不存在");
        }
        Long userId = UserHolder.getUser().getId();
        if (!userId.equals(comment.getUserId())) {
            return Result.fail("只能删除自己的评论");
        }
        removeById(id);
        return Result.ok();
    }
}
