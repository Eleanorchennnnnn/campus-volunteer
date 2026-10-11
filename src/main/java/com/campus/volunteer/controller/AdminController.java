package com.campus.volunteer.controller;

import com.campus.volunteer.dto.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员接口
 * 第三步暂以 ping 验证角色拦截器，后续步骤填充用户管理等真实接口
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/ping")
    public Result ping() {
        return Result.ok("管理员接口连通");
    }
}
