package com.campus.volunteer.config;

import com.campus.volunteer.dto.UserDTO;
import com.campus.volunteer.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 角色拦截器（在登录拦截器之后执行）
 * /admin/** 要求管理员（role=2）；/org/** 要求组织方及以上（role>=1）
 */
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserDTO user = UserHolder.getUser();
        // 双保险：正常情况下登录拦截器已保证用户存在
        if (user == null) {
            response.setStatus(401);
            return false;
        }
        Integer role = user.getRole();
        String uri = request.getRequestURI();
        if (uri.startsWith("/admin") && (role == null || role < 2)) {
            response.setStatus(403);
            return false;
        }
        if (uri.startsWith("/org") && (role == null || role < 1)) {
            response.setStatus(403);
            return false;
        }
        return true;
    }
}
