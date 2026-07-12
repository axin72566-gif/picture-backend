package com.example.picturebackend.space.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.space.constant.SpaceRole;
import com.example.picturebackend.space.entity.Space;
import com.example.picturebackend.space.entity.SpaceMember;
import com.example.picturebackend.space.mapper.SpaceInviteMapper;
import com.example.picturebackend.space.mapper.SpaceMapper;
import com.example.picturebackend.space.mapper.SpaceMemberMapper;
import com.example.picturebackend.space.model.converter.SpaceConverter;
import com.example.picturebackend.space.model.dto.SpaceCreateRequest;
import com.example.picturebackend.space.model.dto.SpaceUpdateRequest;
import com.example.picturebackend.space.model.vo.SpaceVO;
import com.example.picturebackend.space.service.SpaceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SpaceServiceImpl implements SpaceService {

    private static final int MAX_NAME_LENGTH = 64;

    private static final int MAX_DESCRIPTION_LENGTH = 512;

    private final SpaceMapper spaceMapper;

    private final SpaceMemberMapper spaceMemberMapper;

    private final SpaceInviteMapper spaceInviteMapper;

    public SpaceServiceImpl(SpaceMapper spaceMapper,
                            SpaceMemberMapper spaceMemberMapper,
                            SpaceInviteMapper spaceInviteMapper) {
        this.spaceMapper = spaceMapper;
        this.spaceMemberMapper = spaceMemberMapper;
        this.spaceInviteMapper = spaceInviteMapper;
    }

    @Override
    @Transactional
    public SpaceVO createSpace(SpaceCreateRequest request, Long ownerId) {
        if (ownerId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
        }
        String name = request.getName().trim();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        String description = normalizeDescription(request.getDescription());

        Space space = new Space();
        space.setName(name);
        space.setDescription(description);
        space.setOwnerId(ownerId);
        int rows = spaceMapper.insert(space);
        if (rows <= 0 || space.getId() == null) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "创建空间失败");
        }

        SpaceMember member = new SpaceMember();
        member.setSpaceId(space.getId());
        member.setUserId(ownerId);
        member.setRole(SpaceRole.CREATOR);
        int memberRows = spaceMemberMapper.insert(member);
        if (memberRows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "创建空间失败");
        }

        SpaceVO vo = SpaceConverter.toVO(space);
        vo.setMyRole(SpaceRole.CREATOR);
        return vo;
    }

    @Override
    @Transactional
    public SpaceVO updateSpace(Long spaceId, SpaceUpdateRequest request, Long operatorId) {
        requireCreator(spaceId, operatorId);
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求不能为空");
        }

        Space space = requireSpace(spaceId);
        boolean changed = false;
        if (StringUtils.hasText(request.getName())) {
            String name = request.getName().trim();
            if (name.length() > MAX_NAME_LENGTH) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
            }
            space.setName(name);
            changed = true;
        }
        if (request.getDescription() != null) {
            space.setDescription(normalizeDescription(request.getDescription()));
            changed = true;
        }
        if (!changed) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有可更新的字段");
        }

        int rows = spaceMapper.updateById(space);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "更新空间失败");
        }
        SpaceVO vo = SpaceConverter.toVO(requireSpace(spaceId));
        vo.setMyRole(SpaceRole.CREATOR);
        return vo;
    }

    @Override
    @Transactional
    public void dissolveSpace(Long spaceId, Long operatorId) {
        requireCreator(spaceId, operatorId);
        Space space = requireSpace(spaceId);
        int rows = spaceMapper.deleteById(space.getId());
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "解散空间失败");
        }
        spaceMemberMapper.deleteAllBySpaceIdPhysically(spaceId);
        spaceInviteMapper.deletePendingBySpaceIdPhysically(spaceId);
    }

    @Override
    public IPage<SpaceVO> pageMySpaces(Long userId, PageRequest pageRequest) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        PageRequest req = pageRequest != null ? pageRequest : new PageRequest();
        Page<SpaceMember> page = new Page<>(req.getCurrent(), req.getPageSize());
        Page<SpaceMember> memberPage = spaceMemberMapper.selectPage(page, new LambdaQueryWrapper<SpaceMember>()
                .eq(SpaceMember::getUserId, userId)
                .eq(SpaceMember::getIsDelete, 0)
                .orderByDesc(SpaceMember::getCreateTime));

        List<SpaceMember> members = memberPage.getRecords();
        if (members == null || members.isEmpty()) {
            IPage<SpaceVO> empty = new Page<>(memberPage.getCurrent(), memberPage.getSize(), memberPage.getTotal());
            empty.setRecords(List.of());
            return empty;
        }

        Set<Long> spaceIds = members.stream().map(SpaceMember::getSpaceId).collect(Collectors.toSet());
        Map<Long, Space> spaceMap = spaceMapper.selectBatchIds(spaceIds).stream()
                .filter(s -> s != null && !Objects.equals(s.getIsDelete(), 1))
                .collect(Collectors.toMap(Space::getId, Function.identity(), (a, b) -> a));
        Map<Long, String> roleMap = members.stream()
                .collect(Collectors.toMap(SpaceMember::getSpaceId, SpaceMember::getRole, (a, b) -> a));

        List<SpaceVO> voList = members.stream()
                .map(m -> {
                    Space space = spaceMap.get(m.getSpaceId());
                    if (space == null) {
                        return null;
                    }
                    SpaceVO vo = SpaceConverter.toVO(space);
                    vo.setMyRole(roleMap.get(m.getSpaceId()));
                    return vo;
                })
                .filter(Objects::nonNull)
                .toList();

        IPage<SpaceVO> result = new Page<>(memberPage.getCurrent(), memberPage.getSize(), memberPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    @Override
    public SpaceVO getSpace(Long spaceId, Long userId) {
        Space space = requireSpace(spaceId);
        SpaceMember member = requireMember(spaceId, userId);
        SpaceVO vo = SpaceConverter.toVO(space);
        vo.setMyRole(member.getRole());
        return vo;
    }

    @Override
    public Space requireSpace(Long spaceId) {
        if (spaceId == null || spaceId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 ID 不能为空");
        }
        Space space = spaceMapper.selectById(spaceId);
        if (space == null || Objects.equals(space.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间不存在");
        }
        return space;
    }

    @Override
    public SpaceMember requireMember(Long spaceId, Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        SpaceMember member = spaceMemberMapper.selectOne(new LambdaQueryWrapper<SpaceMember>()
                .eq(SpaceMember::getSpaceId, spaceId)
                .eq(SpaceMember::getUserId, userId)
                .eq(SpaceMember::getIsDelete, 0)
                .last("LIMIT 1"));
        if (member == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, "不是该空间成员");
        }
        return member;
    }

    @Override
    public void requireCreator(Long spaceId, Long userId) {
        SpaceMember member = requireMember(spaceId, userId);
        if (!SpaceRole.CREATOR.equals(member.getRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "仅创建者可操作");
        }
    }

    @Override
    public boolean isMember(Long spaceId, Long userId) {
        if (spaceId == null || userId == null) {
            return false;
        }
        Long count = spaceMemberMapper.selectCount(new LambdaQueryWrapper<SpaceMember>()
                .eq(SpaceMember::getSpaceId, spaceId)
                .eq(SpaceMember::getUserId, userId)
                .eq(SpaceMember::getIsDelete, 0));
        return count != null && count > 0;
    }

    private static String normalizeDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        String trimmed = description.trim();
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间简介过长");
        }
        return trimmed;
    }
}
