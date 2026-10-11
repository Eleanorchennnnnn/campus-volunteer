package com.campus.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动实体
 * 由黑马点评的 tb_shop + tb_voucher + tb_seckill_voucher 三表融合而来：
 * 地点/图文来自商户表，名额(quota)与报名窗口来自秒杀券表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_activity")
public class Activity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 主办方组织ID
     */
    private Long orgId;

    /**
     * 活动分类ID
     */
    private Long typeId;

    /**
     * 活动标题
     */
    private String title;

    /**
     * 活动图片，多张逗号分隔
     */
    private String images;

    /**
     * 活动详情（图文）
     */
    private String content;

    /**
     * 活动地点
     */
    private String address;

    /**
     * 经度（GEO 附近活动预留，二期启用）
     */
    private BigDecimal x;

    /**
     * 纬度（GEO 附近活动预留，二期启用）
     */
    private BigDecimal y;

    /**
     * 活动开始时间
     */
    private LocalDateTime beginTime;

    /**
     * 活动结束时间
     */
    private LocalDateTime endTime;

    /**
     * 报名开始时间（NULL=发布即可报名）
     */
    private LocalDateTime enrollStartTime;

    /**
     * 报名截止时间
     */
    private LocalDateTime enrollEndTime;

    /**
     * 总名额（0=不限额）
     */
    private Integer totalQuota;

    /**
     * 剩余名额
     */
    private Integer leftQuota;

    /**
     * 可认定志愿时长（小时）
     */
    private BigDecimal volunteerHours;

    /**
     * 状态：0-草稿 1-报名中 2-进行中 3-已结束 4-已取消
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    // ---------- 非表字段：查询时富化，不缓存 ----------

    /**
     * 主办方组织名称（非表字段）
     */
    @TableField(exist = false)
    private String orgName;

    /**
     * 分类名称（非表字段）
     */
    @TableField(exist = false)
    private String typeName;
}
