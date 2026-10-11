package com.campus.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户详情实体（志愿者画像）
 * 由黑马点评的城市/生日/会员等级等社交字段改造为志愿时长画像
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_user_info")
public class UserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键，用户id（对应 tb_user.id，非自增）
     */
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    /**
     * 累计志愿时长（小时）
     */
    private BigDecimal totalHours;

    /**
     * 志愿积分
     */
    private Integer credits;

    /**
     * 志愿等级：0-4 星级志愿者
     */
    private Integer level;

    /**
     * 粉丝数
     */
    private Integer fans;

    /**
     * 关注数
     */
    private Integer follows;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
