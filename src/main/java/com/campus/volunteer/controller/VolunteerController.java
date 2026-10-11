package com.campus.volunteer.controller;

import com.campus.volunteer.dto.Result;
import com.campus.volunteer.service.IVolunteerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 志愿服务控制器
 * /volunteer/rank 公开（MvcConfig已排除登录拦截），其余接口需登录
 */
@RestController
@RequestMapping("/volunteer")
public class VolunteerController {

    @Resource
    private IVolunteerService volunteerService;

    /**
     * 志愿时长排行榜（ZSet Top10，公开）
     */
    @GetMapping("/rank")
    public Result rank() {
        return volunteerService.rank();
    }

    /**
     * 我的志愿档案：累计时长/积分/星级/排名
     */
    @GetMapping("/me")
    public Result myProfile() {
        return volunteerService.myProfile();
    }

    /**
     * 志愿服务证明数据（前端渲染打印版，二期可升级PDF）
     */
    @GetMapping("/certificate")
    public Result certificate() {
        return volunteerService.certificate();
    }
}
