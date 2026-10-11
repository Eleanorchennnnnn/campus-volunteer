package com.campus.volunteer.dto;

import lombok.Data;

/**
 * Redis Hash 缓存的登录用户信息
 */
@Data
public class UserDTO {
    private Long id;
    private String nickName;
    private String icon;
    /**
     * 角色：0-学生 1-组织方 2-管理员
     */
    private Integer role;
}
