package com.campus.volunteer.config;

import com.campus.volunteer.utils.LoginInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import java.io.File;

/**
 * Web 配置：
 * 1. 拦截器三层：刷新Token(0) → 登录校验(1) → 角色门槛(2)
 * 2. 静态资源：/uploads/** 映射到本地图片上传目录
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** 图片上传目录，与 UploadController 保持一致 */
    @Value("${upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate))
                .addPathPatterns("/**")
                .order(0);
        registry.addInterceptor(new LoginInterceptor(stringRedisTemplate))
                .excludePathPatterns(
                        // 登录接口
                        "/user/code",
                        "/user/login",
                        // 公开浏览：活动/分类/志愿榜/帖子/评论列表/静态图片
                        "/activity/**",
                        "/activity-type/**",
                        "/volunteer/rank",
                        "/post/*",
                        "/post/of/activity/**",
                        "/post-comment/of/**",
                        "/uploads/**"
                )
                .order(1);
        registry.addInterceptor(new RoleInterceptor())
                .addPathPatterns("/admin/**", "/org/**")
                .order(2);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /uploads/** 映射到本地磁盘目录，供帖子图片等静态资源访问
        String absolutePath = new File(uploadDir).getAbsolutePath().replace("\\", "/");
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}
