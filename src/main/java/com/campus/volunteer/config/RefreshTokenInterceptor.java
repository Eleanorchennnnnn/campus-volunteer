package com.campus.volunteer.config;

import cn.hutool.core.bean.BeanUtil;
import com.campus.volunteer.dto.LoginFormDTO;
import com.campus.volunteer.dto.UserDTO;
import com.campus.volunteer.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.campus.volunteer.utils.RedisConstants.LOGIN_USER_KEY;
import static com.campus.volunteer.utils.RedisConstants.LOGIN_USER_TTL;

public class RefreshTokenInterceptor implements HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;
    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    @Override
    public boolean preHandle (HttpServletRequest request, HttpServletResponse response,Object handler){
        //1.从请求头中获取token
        String token = request.getHeader("authorization");
        if(token == null){
            return true;
            //没token就放行，后面LoginInterceptor会拦截
            //查看一些不需要登陆就能看的页面
        }
        //2.从Redis 中获取用户信息
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash()
                .entries(LOGIN_USER_KEY + token);
        //3.如果Redis里有用户信息，就把UserDTO存到ThreadLocal
        if(!userMap.isEmpty()){
            //刷新token有效期（用户还在操作）
            stringRedisTemplate.expire(LOGIN_USER_KEY + token,LOGIN_USER_TTL, TimeUnit.MINUTES);
            UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap,new UserDTO(),false);
            UserHolder.saveUser(userDTO);
        }
        return true;//始终放行，只负责刷新，不负责拦截
    }
    @Override
    public void afterCompletion(HttpServletRequest request,HttpServletResponse response,Object handler,Exception ex){
        //请求结束后清除ThreadLocal，避免内存泄露
        UserHolder.removeUser();
    }
}
