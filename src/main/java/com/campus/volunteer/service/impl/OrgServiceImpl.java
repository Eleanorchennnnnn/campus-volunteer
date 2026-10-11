package com.campus.volunteer.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.volunteer.entity.Org;
import com.campus.volunteer.mapper.OrgMapper;
import com.campus.volunteer.service.IOrgService;
import org.springframework.stereotype.Service;

/**
 * 组织服务实现（关注/组织管理环节再扩展）
 */
@Service
public class OrgServiceImpl extends ServiceImpl<OrgMapper, Org> implements IOrgService {
}
