package com.example.picturebackend.space.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.space.model.vo.SpaceMemberVO;

public interface SpaceMemberService {

    IPage<SpaceMemberVO> pageMembers(Long spaceId, Long operatorId, PageRequest pageRequest);

    void updateRole(Long spaceId, Long targetUserId, String role, Long operatorId);

    void removeMember(Long spaceId, Long targetUserId, Long operatorId);

    void leave(Long spaceId, Long userId);
}
