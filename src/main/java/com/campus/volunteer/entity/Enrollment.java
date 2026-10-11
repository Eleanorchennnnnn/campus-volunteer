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
 * 报名记录实体（由秒杀订单表改造）
 * id 由 RedisIdWorker 生成全局唯一ID（非自增）
 * uk(user_id, activity_id) 唯一索引 = "一人一活动"数据库兜底
 * 取消报名采用"事务内物理删除"策略，重新报名不冲突
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_enrollment")
public class Enrollment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 报名单号（RedisIdWorker 全局ID）
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 状态：0-已报名 1-已签到 2-已完成(时长已认定) 3-已取消
     */
    private Integer status;

    /**
     * 报名时间
     */
    private LocalDateTime enrollTime;

    /**
     * 现场签到时间
     */
    private LocalDateTime signTime;

    /**
     * 完成时间（时长认定时间）
     */
    private LocalDateTime finishTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    // ---------- 非表字段：我的报名列表富化 ----------

    /**
     * 活动标题（非表字段）
     */
    @TableField(exist = false)
    private String activityTitle;

    /**
     * 活动开始时间（非表字段）
     */
    @TableField(exist = false)
    private LocalDateTime activityBeginTime;

    /**
     * 活动地点（非表字段）
     */
    @TableField(exist = false)
    private String activityAddress;
}
