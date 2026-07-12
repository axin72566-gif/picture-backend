package com.example.picturebackend.space.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.space.entity.Space;
import com.example.picturebackend.space.entity.SpaceMember;
import com.example.picturebackend.space.model.dto.SpaceCreateRequest;
import com.example.picturebackend.space.model.dto.SpaceUpdateRequest;
import com.example.picturebackend.space.model.vo.SpaceVO;

public interface SpaceService {

    SpaceVO createSpace(SpaceCreateRequest request, Long ownerId);

    SpaceVO updateSpace(Long spaceId, SpaceUpdateRequest request, Long operatorId);

    void dissolveSpace(Long spaceId, Long operatorId);

    IPage<SpaceVO> pageMySpaces(Long userId, PageRequest pageRequest);

    SpaceVO getSpace(Long spaceId, Long userId);

    Space requireSpace(Long spaceId);

    SpaceMember requireMember(Long spaceId, Long userId);

    /**
     * 要求成员角色不低于 minRole（VIEWER / EDITOR / CREATOR）。
     */
    SpaceMember requireRoleAtLeast(Long spaceId, Long userId, String minRole);

    void requireCreator(Long spaceId, Long userId);

    boolean isMember(Long spaceId, Long userId);
}
