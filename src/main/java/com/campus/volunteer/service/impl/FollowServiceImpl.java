package com.campus.volunteer.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.volunteer.dto.Result;
import com.campus.volunteer.entity.Follow;
import com.campus.volunteer.entity.Org;
import com.campus.volunteer.mapper.FollowMapper;
import com.campus.volunteer.service.IFollowService;
import com.campus.volunteer.service.IOrgService;
import com.campus.volunteer.utils.UserHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 关注服务实现：关注社团/组织（由课程"关注用户"改造）
 * 组织必须存在 + 防重复关注；二期可扩展共同关注（Set交集）、Feed推送
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private IOrgService orgService;

    @Override
    public Result followOrg(Long orgId) {
        Long userId = UserHolder.getUser().getId();
        // 1.组织必须存在
        Org org = orgService.getById(orgId);
        if (org == null) {
            return Result.fail("组织不存在");
        }
        // 2.防重复关注（MyBatis-Plus 3.4.3 的 count() 返回 Integer）
        Integer count = query().eq("user_id", userId).eq("follow_org_id", orgId).count();
        if (count != null && count > 0) {
            return Result.fail("请勿重复关注");
        }
        // 3.落库
        Follow follow = new Follow().setUserId(userId).setFollowOrgId(orgId);
        save(follow);
        return Result.ok();
    }

    @Override
    public Result unFollowOrg(Long orgId) {
        Long userId = UserHolder.getUser().getId();
        boolean removed = remove(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Follow>()
                        .eq("user_id", userId)
                        .eq("follow_org_id", orgId));
        return removed ? Result.ok() : Result.fail("未关注该组织");
    }

    @Override
    public Result isFollowed(Long orgId) {
        Long userId = UserHolder.getUser().getId();
        Integer count = query().eq("user_id", userId).eq("follow_org_id", orgId).count();
        return Result.ok(count != null && count > 0);
    }
}
