package com.campus.volunteer.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.volunteer.entity.UserInfo;
import com.campus.volunteer.mapper.UserInfoMapper;
import com.campus.volunteer.service.IUserInfoService;
import org.springframework.stereotype.Service;

/**
 * 用户详情服务实现（志愿时长认定环节再扩展写入逻辑）
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {
}
