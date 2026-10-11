package com.campus.volunteer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.volunteer.dto.LoginFormDTO;
import com.campus.volunteer.dto.Result;
import com.campus.volunteer.dto.UserDTO;
import com.campus.volunteer.entity.User;
import com.campus.volunteer.mapper.UserMapper;
import com.campus.volunteer.service.IUserService;
import com.campus.volunteer.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.campus.volunteer.utils.RedisConstants.*;

/**
 * 用户服务实现：学号 + 验证码登录，会话存 Redis Hash
 * 与黑马点评原实现的差异：
 * 1. 登录标识由手机号改为学号，验证码 key 跟随学号
 * 2. 取消"不存在则自动注册"——校园账号由管理员预导入
 * 3. 修复原代码 token 有效期误用 LOGIN_CODE_TTL（2分钟）的 bug，改为 LOGIN_USER_TTL
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String studentNo) {
        // 1. 校验学号格式
        if (RegexUtils.isStudentNoInvalid(studentNo)) {
            return Result.fail("学号格式错误");
        }
        // 2. 查询用户是否存在（校园账号预导入，不自动注册）
        User user = query().eq("student_no", studentNo).one();
        if (user == null) {
            return Result.fail("学号不存在，请联系管理员导入");
        }
        // 3. 生成6位数字验证码，存入Redis，有效期2分钟
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + studentNo, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        // 4. 发送验证码（真实项目对接短信平台，这里以日志模拟）
        log.info("向 {}({}) 的手机 {} 发送验证码：{}", user.getNickName(), user.getStudentNo(), user.getPhone(), code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm) {
        // 1. 校验学号
        String studentNo = loginForm.getStudentNo();
        if (RegexUtils.isStudentNoInvalid(studentNo)) {
            return Result.fail("学号格式错误");
        }
        // 2. 从Redis取出验证码与请求比对
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + studentNo);
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.equals(code)) {
            return Result.fail("验证码错误或已过期");
        }
        // 3. 一致则删除验证码（一次性使用，防止重放）
        stringRedisTemplate.delete(LOGIN_CODE_KEY + studentNo);
        // 4. 根据学号查询用户
        User user = query().eq("student_no", studentNo).one();
        if (user == null) {
            return Result.fail("用户不存在");
        }
        // 5. 生成token，将UserDTO转为Hash存入Redis（role 随会话下发）
        String token = UUID.randomUUID().toString();
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", userDTO.getId().toString());
        userMap.put("nickName", userDTO.getNickName());
        userMap.put("icon", userDTO.getIcon() == null ? "" : userDTO.getIcon());
        userMap.put("role", userDTO.getRole() == null ? "0" : userDTO.getRole().toString());
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        // 6. 设置token有效期：LOGIN_USER_TTL（原代码此处误用 LOGIN_CODE_TTL，已修复）
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
        // 7. 返回token
        return Result.ok(token);
    }
}
