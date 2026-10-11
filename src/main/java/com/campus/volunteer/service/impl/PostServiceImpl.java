package com.campus.volunteer.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.volunteer.dto.Result;
import com.campus.volunteer.dto.UserDTO;
import com.campus.volunteer.entity.Post;
import com.campus.volunteer.entity.PostLike;
import com.campus.volunteer.entity.User;
import com.campus.volunteer.mapper.PostLikeMapper;
import com.campus.volunteer.mapper.PostMapper;
import com.campus.volunteer.service.IActivityService;
import com.campus.volunteer.service.IPostService;
import com.campus.volunteer.service.IUserService;
import com.campus.volunteer.utils.SystemConstants;
import com.campus.volunteer.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.campus.volunteer.utils.RedisConstants.POST_LIKED_KEY;

/**
 * 帖子服务实现
 *
 * 点赞方案（课程"Set一人一赞"版的校园迁移）：
 * - 防重与点赞记录：Redis Set post:liked:{postId}，member=用户ID
 * - 计数冗余：tb_post.liked 列，随点赞/取消同步增减
 * - 相对原BlogController裸奔的 liked+1（可无限刷赞），这里一个用户对一个帖子只有一票
 * - 冷启动：Set为空时从tb_post_like历史表初始化（种子数据/Redis重启兜底），
 *   空点赞集合用哨兵成员"0"标记已初始化，避免反复查库
 */
@Slf4j
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements IPostService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;

    @Resource
    private IActivityService activityService;

    @Resource
    private PostLikeMapper postLikeMapper;

    @Override
    public Result savePost(Post post) {
        Long userId = UserHolder.getUser().getId();
        // 1.参数校验
        if (StrUtil.isBlank(post.getContent())) {
            return Result.fail("帖子内容不能为空");
        }
        if (post.getTitle() != null && post.getTitle().length() > 128) {
            return Result.fail("标题过长");
        }
        if (post.getContent().length() > 2048) {
            return Result.fail("内容过长");
        }
        // 2.关联活动校验（可选）
        if (post.getActivityId() != null && activityService.getById(post.getActivityId()) == null) {
            return Result.fail("关联的活动不存在");
        }
        // 3.落库
        post.setUserId(userId);
        post.setLiked(0);
        save(post);
        return Result.ok(post.getId());
    }

    @Override
    public Result queryById(Long id) {
        Post post = getById(id);
        if (post == null) {
            return Result.fail("帖子不存在");
        }
        enrich(post);
        return Result.ok(post);
    }

    @Override
    public Result likePost(Long id) {
        Long userId = UserHolder.getUser().getId();
        Post post = getById(id);
        if (post == null) {
            return Result.fail("帖子不存在");
        }
        String key = POST_LIKED_KEY + id;
        ensureLikedInit(id);
        Boolean liked = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        boolean likedNow;
        if (Boolean.TRUE.equals(liked)) {
            // 已点赞 → 取消（liked列带 gt(0) 防负数）
            stringRedisTemplate.opsForSet().remove(key, userId.toString());
            update().setSql("liked = liked - 1").eq("id", id).gt("liked", 0).update();
            likedNow = false;
        } else {
            // 未点赞 → 点赞
            stringRedisTemplate.opsForSet().add(key, userId.toString());
            update().setSql("liked = liked + 1").eq("id", id).update();
            likedNow = true;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("liked", likedNow);
        data.put("likedCount", getById(id).getLiked());
        return Result.ok(data);
    }

    @Override
    public Result queryHotPosts(Integer current) {
        Page<Post> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        page.getRecords().forEach(this::enrich);
        return Result.ok(page.getRecords());
    }

    @Override
    public Result queryMyPosts(Integer current) {
        Long userId = UserHolder.getUser().getId();
        Page<Post> page = query()
                .eq("user_id", userId)
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        page.getRecords().forEach(this::enrich);
        return Result.ok(page.getRecords());
    }

    @Override
    public Result queryPostsOfActivity(Long activityId, Integer current) {
        Page<Post> page = query()
                .eq("activity_id", activityId)
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        page.getRecords().forEach(this::enrich);
        return Result.ok(page.getRecords());
    }

    /**
     * 富化：作者昵称/头像 + 当前用户是否点赞（未登录视为false）
     */
    private void enrich(Post post) {
        User user = userService.getById(post.getUserId());
        if (user != null) {
            post.setName(user.getNickName());
            post.setIcon(user.getIcon());
        }
        UserDTO me = UserHolder.getUser();
        if (me != null) {
            ensureLikedInit(post.getId());
            Boolean liked = stringRedisTemplate.opsForSet()
                    .isMember(POST_LIKED_KEY + post.getId(), me.getId().toString());
            post.setIsLiked(Boolean.TRUE.equals(liked));
        }
    }

    /**
     * 点赞集合冷启动：Set为空时从tb_post_like历史表初始化
     * 空点赞集合写入哨兵成员"0"标记"已初始化"，避免每次浏览都查库
     */
    private void ensureLikedInit(Long postId) {
        String key = POST_LIKED_KEY + postId;
        Long size = stringRedisTemplate.opsForSet().size(key);
        if (size != null && size > 0) {
            return;
        }
        List<PostLike> likes = postLikeMapper.selectList(
                new QueryWrapper<PostLike>().eq("post_id", postId));
        for (PostLike like : likes) {
            stringRedisTemplate.opsForSet().add(key, like.getUserId().toString());
        }
        if (likes.isEmpty()) {
            stringRedisTemplate.opsForSet().add(key, "0");
        }
        log.debug("帖子{}：点赞集合冷启动初始化完成", postId);
    }
}
