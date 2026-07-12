package com.example.picturebackend.space.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.space.constant.SpaceRole;
import com.example.picturebackend.space.entity.SpaceMember;
import com.example.picturebackend.space.mapper.SpaceMemberMapper;
import com.example.picturebackend.space.model.vo.SpaceMemberVO;
import com.example.picturebackend.space.service.SpaceMemberService;
import com.example.picturebackend.space.service.SpaceService;
import com.example.picturebackend.user.entity.User;
import com.example.picturebackend.user.mapper.UserMapper;
import com.example.picturebackend.user.model.converter.UserConverter;
import com.example.picturebackend.user.model.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SpaceMemberServiceImpl implements SpaceMemberService {

    private final SpaceMemberMapper spaceMemberMapper;

    private final UserMapper userMapper;

    private final SpaceService spaceService;

    public SpaceMemberServiceImpl(SpaceMemberMapper spaceMemberMapper,
                                  UserMapper userMapper,
                                  SpaceService spaceService) {
        this.spaceMemberMapper = spaceMemberMapper;
        this.userMapper = userMapper;
        this.spaceService = spaceService;
    }

    @Override
    public IPage<SpaceMemberVO> pageMembers(Long spaceId, Long operatorId, PageRequest pageRequest) {
        spaceService.requireMember(spaceId, operatorId);
        spaceService.requireSpace(spaceId);

        PageRequest req = pageRequest != null ? pageRequest : new PageRequest();
        Page<SpaceMember> page = new Page<>(req.getCurrent(), req.getPageSize());
        Page<SpaceMember> result = spaceMemberMapper.selectPage(page, new LambdaQueryWrapper<SpaceMember>()
                .eq(SpaceMember::getSpaceId, spaceId)
                .eq(SpaceMember::getIsDelete, 0)
                .orderByAsc(SpaceMember::getCreateTime));

        return toVoPage(result);
    }

    @Override
    @Transactional
    public void updateRole(Long spaceId, Long targetUserId, String role, Long operatorId) {
        spaceService.requireCreator(spaceId, operatorId);
        if (targetUserId == null || targetUserId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标用户 ID 不能为空");
        }
        if (!StringUtils.hasText(role) || !SpaceRole.isAssignable(role)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色仅支持 EDITOR 或 VIEWER");
        }
        SpaceMember target = spaceService.requireMember(spaceId, targetUserId);
        if (SpaceRole.CREATOR.equals(target.getRole())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能修改创建者角色");
        }
        if (Objects.equals(target.getRole(), role)) {
            return;
        }
        SpaceMember update = new SpaceMember();
        update.setId(target.getId());
        update.setRole(role);
        int rows = spaceMemberMapper.updateById(update);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "修改角色失败");
        }
    }

    @Override
    @Transactional
    public void removeMember(Long spaceId, Long targetUserId, Long operatorId) {
        spaceService.requireCreator(spaceId, operatorId);
        if (targetUserId == null || targetUserId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标用户 ID 不能为空");
        }
        SpaceMember target = spaceService.requireMember(spaceId, targetUserId);
        if (SpaceRole.CREATOR.equals(target.getRole())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能移除创建者");
        }
        int rows = spaceMemberMapper.deletePhysically(spaceId, targetUserId);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "成员不存在");
        }
    }

    @Override
    @Transactional
    public void leave(Long spaceId, Long userId) {
        SpaceMember member = spaceService.requireMember(spaceId, userId);
        if (SpaceRole.CREATOR.equals(member.getRole())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建者不能退出，请先解散空间");
        }
        int rows = spaceMemberMapper.deletePhysically(spaceId, userId);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未加入该空间");
        }
    }

    private IPage<SpaceMemberVO> toVoPage(Page<SpaceMember> result) {
        List<SpaceMember> records = result.getRecords();
        if (records == null || records.isEmpty()) {
            IPage<SpaceMemberVO> empty = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
            empty.setRecords(List.of());
            return empty;
        }

        Set<Long> userIds = records.stream().map(SpaceMember::getUserId).collect(Collectors.toSet());
        Map<Long, UserVO> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, UserConverter::toVO, (a, b) -> a));

        List<SpaceMemberVO> voList = records.stream().map(member -> {
            SpaceMemberVO vo = new SpaceMemberVO();
            vo.setId(member.getId());
            vo.setSpaceId(member.getSpaceId());
            vo.setRole(member.getRole());
            vo.setCreateTime(member.getCreateTime());
            vo.setUser(userMap.get(member.getUserId()));
            return vo;
        }).toList();

        IPage<SpaceMemberVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }
}
