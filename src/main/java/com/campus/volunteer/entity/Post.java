package com.campus.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 帖子实体：活动回顾帖/志愿心得（由探店笔记改造，新增 activityId 关联）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_post")
public class Post implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 发布者用户ID
     */
    private Long userId;

    /**
     * 关联活动ID（NULL=普通分享）
     */
    private Long activityId;

    /**
     * 标题
     */
    private String title;

    /**
     * 正文
     */
    private String content;

    /**
     * 图片，多张逗号分隔
     */
    private String images;

    /**
     * 点赞数（冗余计数，防重记录在Redis Set）
     */
    private Integer liked;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    // ---------- 非表字段：查询时富化 ----------

    /**
     * 作者昵称（非表字段）
     */
    @TableField(exist = false)
    private String name;

    /**
     * 作者头像（非表字段）
     */
    @TableField(exist = false)
    private String icon;

    /**
     * 当前用户是否已点赞（非表字段）
     */
    @TableField(exist = false)
    private Boolean isLiked;
}
