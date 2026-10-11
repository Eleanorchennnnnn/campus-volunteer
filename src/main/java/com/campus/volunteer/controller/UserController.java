package com.campus.volunteer.controller;

import com.campus.volunteer.dto.LoginFormDTO;
import com.campus.volunteer.dto.Result;
import com.campus.volunteer.dto.UserDTO;
import com.campus.volunteer.entity.UserInfo;
import com.campus.volunteer.service.IUserInfoService;
import com.campus.volunteer.service.IUserService;
import com.campus.volunteer.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

import static com.campus.volunteer.utils.RedisConstants.LOGIN_USER_KEY;

/**
 * 用户控制器：学号验证码登录 / 会话管理 / 志愿者画像查询
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发送验证码（按学号）
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("studentNo") String studentNo) {
        return userService.sendCode(studentNo);
    }

    /**
     * 登录：学号 + 验证码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm) {
        return userService.login(loginForm);
    }

    /**
     * 登出：删除Redis中的token并清理ThreadLocal
     */
    @PostMapping("/logout")
    public Result logout(@RequestHeader("authorization") String token) {
        stringRedisTemplate.delete(LOGIN_USER_KEY + token);
        UserHolder.removeUser();
        return Result.ok();
    }

    /**
     * 当前登录用户
     */
    @GetMapping("/me")
    public Result me() {
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    /**
     * 查询用户志愿者画像（累计时长/积分/等级等）
     */
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId) {
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        return Result.ok(info);
    }
}
