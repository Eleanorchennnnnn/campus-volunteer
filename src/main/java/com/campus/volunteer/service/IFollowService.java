package com.campus.volunteer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.volunteer.dto.Result;
import com.campus.volunteer.entity.Follow;

/**
 * 关注服务接口：关注社团/组织
 */
public interface IFollowService extends IService<Follow> {

    /**
     * 关注组织
     */
    Result followOrg(Long orgId);

    /**
     * 取消关注组织
     */
    Result unFollowOrg(Long orgId);

    /**
     * 查询是否已关注该组织
     */
    Result isFollowed(Long orgId);
}
