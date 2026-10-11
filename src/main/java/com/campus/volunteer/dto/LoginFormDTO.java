package com.campus.volunteer.dto;

import lombok.Data;

/**
 * 登录表单：学号 + 验证码
 */
@Data
public class LoginFormDTO {
    private String studentNo;
    private String code;
}
