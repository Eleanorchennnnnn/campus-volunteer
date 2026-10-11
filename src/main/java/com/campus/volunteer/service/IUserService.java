package com.campus.volunteer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.volunteer.dto.LoginFormDTO;
import com.campus.volunteer.dto.Result;
import com.campus.volunteer.entity.User;

/**
 * 用户服务接口：学号 + 验证码登录
 */
public interface IUserService extends IService<User> {

    /**
     * 发送验证码（按学号查用户，向其手机号发送）
     */
    Result sendCode(String studentNo);

    /**
     * 登录，成功返回 token
     */
    Result login(LoginFormDTO loginForm);
}
