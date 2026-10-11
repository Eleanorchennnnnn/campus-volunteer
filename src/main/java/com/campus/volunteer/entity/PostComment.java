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
 * 帖子评论实体（由探店笔记评论改造，支持两级评论）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_post_comment")
public class PostComment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 评论者用户ID
     */
    private Long userId;

    /**
     * 父评论ID（顶级评论为NULL）
     */
    private Long parentId;

    /**
     * 回复目标用户ID
     */
    private Long answerId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 点赞数
     */
    private Integer liked;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    // ---------- 非表字段 ----------

    /**
     * 评论者昵称（非表字段）
     */
    @TableField(exist = false)
    private String nickName;

    /**
     * 评论者头像（非表字段）
     */
    @TableField(exist = false)
    private String icon;
}
