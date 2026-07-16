package com.example.picturebackend.vip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.space.constant.SpaceInviteStatus;
import com.example.picturebackend.space.entity.Space;
import com.example.picturebackend.space.entity.SpaceInvite;
import com.example.picturebackend.space.entity.SpaceMember;
import com.example.picturebackend.space.mapper.SpaceInviteMapper;
import com.example.picturebackend.space.mapper.SpaceMapper;
import com.example.picturebackend.space.mapper.SpaceMemberMapper;
import com.example.picturebackend.user.entity.User;
import com.example.picturebackend.user.mapper.UserMapper;
import com.example.picturebackend.vip.constant.VipConstant;
import com.example.picturebackend.vip.service.VipQuotaService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class VipQuotaServiceImpl implements VipQuotaService {

    private final UserMapper userMapper;

    private final SpaceMapper spaceMapper;

    private final SpaceMemberMapper spaceMemberMapper;

    private final SpaceInviteMapper spaceInviteMapper;

    public VipQuotaServiceImpl(UserMapper userMapper,
                               SpaceMapper spaceMapper,
                               SpaceMemberMapper spaceMemberMapper,
                               SpaceInviteMapper spaceInviteMapper) {
        this.userMapper = userMapper;
        this.spaceMapper = spaceMapper;
        this.spaceMemberMapper = spaceMemberMapper;
        this.spaceInviteMapper = spaceInviteMapper;
    }

    @Override
    public boolean isVipActive(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userMapper.selectById(userId);
        return isVipActive(user);
    }

    public static boolean isVipActive(User user) {
        if (user == null || user.getVipExpireTime() == null) {
            return false;
        }
        return user.getVipExpireTime().isAfter(LocalDateTime.now());
    }

    @Override
    public int maxOwnedSpaces(Long userId) {
        return isVipActive(userId) ? VipConstant.VIP_MAX_OWNED_SPACES : VipConstant.FREE_MAX_OWNED_SPACES;
    }

    @Override
    public int maxMembersPerSpace(Long ownerUserId) {
        return isVipActive(ownerUserId) ? VipConstant.VIP_MAX_MEMBERS_PER_SPACE : VipConstant.FREE_MAX_MEMBERS_PER_SPACE;
    }

    @Override
    public void requireCanCreateSpace(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Long count = spaceMapper.selectCount(new LambdaQueryWrapper<Space>()
                .eq(Space::getOwnerId, userId)
                .eq(Space::getIsDelete, 0));
        long owned = count == null ? 0L : count;
        int max = maxOwnedSpaces(userId);
        if (owned >= max) {
            throw new BusinessException(ErrorCode.VIP_SPACE_QUOTA_EXCEEDED,
                    "可创建空间数已达上限（" + max + "），请开通或续期 VIP");
        }
    }

    @Override
    public void requireCanInviteMember(Long spaceId) {
        Space space = requireSpace(spaceId);
        int max = maxMembersPerSpace(space.getOwnerId());
        long used = countMembers(spaceId) + countPendingInvites(spaceId);
        if (used >= max) {
            throw new BusinessException(ErrorCode.VIP_MEMBER_QUOTA_EXCEEDED,
                    "空间成员额度已达上限（" + max + "），请开通或续期 VIP");
        }
    }

    @Override
    public void requireCanAcceptMember(Long spaceId) {
        Space space = requireSpace(spaceId);
        int max = maxMembersPerSpace(space.getOwnerId());
        long members = countMembers(spaceId);
        if (members >= max) {
            throw new BusinessException(ErrorCode.VIP_MEMBER_QUOTA_EXCEEDED,
                    "空间成员额度已达上限（" + max + "），请开通或续期 VIP");
        }
    }

    private Space requireSpace(Long spaceId) {
        if (spaceId == null || spaceId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 ID 不能为空");
        }
        Space space = spaceMapper.selectById(spaceId);
        if (space == null || Objects.equals(space.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间不存在");
        }
        return space;
    }

    private long countMembers(Long spaceId) {
        Long memberCount = spaceMemberMapper.selectCount(new LambdaQueryWrapper<SpaceMember>()
                .eq(SpaceMember::getSpaceId, spaceId)
                .eq(SpaceMember::getIsDelete, 0));
        return memberCount == null ? 0L : memberCount;
    }

    private long countPendingInvites(Long spaceId) {
        Long pendingInviteCount = spaceInviteMapper.selectCount(new LambdaQueryWrapper<SpaceInvite>()
                .eq(SpaceInvite::getSpaceId, spaceId)
                .eq(SpaceInvite::getStatus, SpaceInviteStatus.PENDING)
                .eq(SpaceInvite::getIsDelete, 0));
        return pendingInviteCount == null ? 0L : pendingInviteCount;
    }
}
