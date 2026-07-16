package com.example.picturebackend.vip.service;

/**
 * VIP 空间额度查询与校验。
 */
public interface VipQuotaService {

    boolean isVipActive(Long userId);

    int maxOwnedSpaces(Long userId);

    int maxMembersPerSpace(Long ownerUserId);

    /**
     * 校验用户是否还可创建空间；超限抛 VIP_SPACE_QUOTA_EXCEEDED。
     */
    void requireCanCreateSpace(Long userId);

    /**
     * 发起邀请前：成员数 + PENDING 邀请数 ≥ 上限则拒绝。
     */
    void requireCanInviteMember(Long spaceId);

    /**
     * 接受邀请前：当前成员数 ≥ 上限则拒绝（该笔 PENDING 已占用名额，接受时转为成员不额外占额）。
     */
    void requireCanAcceptMember(Long spaceId);
}
