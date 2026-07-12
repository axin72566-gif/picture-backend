package com.example.picturebackend.space.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.space.model.dto.SpaceInviteRequest;
import com.example.picturebackend.space.model.vo.SpaceInviteVO;

public interface SpaceInviteService {

    SpaceInviteVO invite(Long spaceId, SpaceInviteRequest request, Long inviterId);

    IPage<SpaceInviteVO> pageSpacePendingInvites(Long spaceId, Long operatorId, PageRequest pageRequest);

    IPage<SpaceInviteVO> pageMyPendingInvites(Long inviteeId, PageRequest pageRequest);

    void accept(Long inviteId, Long userId);

    void reject(Long inviteId, Long userId);

    void cancel(Long inviteId, Long operatorId);
}
