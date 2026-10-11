package com.campus.volunteer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.volunteer.entity.PostLike;

/**
 * 帖子点赞历史 Mapper（仅用于点赞集合冷启动初始化）
 */
public interface PostLikeMapper extends BaseMapper<PostLike> {
}
