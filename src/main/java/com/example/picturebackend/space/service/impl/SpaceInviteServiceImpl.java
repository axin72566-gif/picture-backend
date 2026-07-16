package com.example.picturebackend.space.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.picturebackend.chat.service.ConversationLifecycleService;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.notification.constant.NotificationType;
import com.example.picturebackend.notification.service.NotificationService;
import com.example.picturebackend.space.constant.SpaceInviteStatus;
import com.example.picturebackend.space.constant.SpaceRole;
import com.example.picturebackend.space.entity.Space;
import com.example.picturebackend.space.entity.SpaceInvite;
import com.example.picturebackend.space.entity.SpaceMember;
import com.example.picturebackend.space.mapper.SpaceInviteMapper;
import com.example.picturebackend.space.mapper.SpaceMapper;
import com.example.picturebackend.space.mapper.SpaceMemberMapper;
import com.example.picturebackend.space.model.dto.SpaceInviteRequest;
import com.example.picturebackend.space.model.vo.SpaceInviteVO;
import com.example.picturebackend.space.service.SpaceInviteService;
import com.example.picturebackend.space.service.SpaceService;
import com.example.picturebackend.user.entity.User;
import com.example.picturebackend.user.mapper.UserMapper;
import com.example.picturebackend.user.model.converter.UserConverter;
import com.example.picturebackend.user.model.vo.UserVO;
import com.example.picturebackend.vip.service.VipQuotaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SpaceInviteServiceImpl implements SpaceInviteService {

    private final SpaceInviteMapper spaceInviteMapper;

    private final SpaceMemberMapper spaceMemberMapper;

    private final SpaceMapper spaceMapper;

    private final UserMapper userMapper;

    private final SpaceService spaceService;

    private final NotificationService notificationService;

    private final ConversationLifecycleService conversationLifecycleService;

    private final VipQuotaService vipQuotaService;

    public SpaceInviteServiceImpl(SpaceInviteMapper spaceInviteMapper,
                                  SpaceMemberMapper spaceMemberMapper,
                                  SpaceMapper spaceMapper,
                                  UserMapper userMapper,
                                  SpaceService spaceService,
                                  NotificationService notificationService,
                                  ConversationLifecycleService conversationLifecycleService,
                                  VipQuotaService vipQuotaService) {
        this.spaceInviteMapper = spaceInviteMapper;
        this.spaceMemberMapper = spaceMemberMapper;
        this.spaceMapper = spaceMapper;
        this.userMapper = userMapper;
        this.spaceService = spaceService;
        this.notificationService = notificationService;
        this.conversationLifecycleService = conversationLifecycleService;
        this.vipQuotaService = vipQuotaService;
    }

    @Override
    @Transactional
    public SpaceInviteVO invite(Long spaceId, SpaceInviteRequest request, Long inviterId) {
        spaceService.requireCreator(spaceId, inviterId);
        Space space = spaceService.requireSpace(spaceId);
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求不能为空");
        }
        if (!StringUtils.hasText(request.getRole()) || !SpaceRole.isAssignable(request.getRole())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邀请角色仅支持 EDITOR 或 VIEWER");
        }

        User invitee = resolveInvitee(request);
        if (Objects.equals(invitee.getId(), inviterId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能邀请自己");
        }
        if (spaceService.isMember(spaceId, invitee.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户已是空间成员");
        }

        Long pendingCount = spaceInviteMapper.selectCount(new LambdaQueryWrapper<SpaceInvite>()
                .eq(SpaceInvite::getSpaceId, spaceId)
                .eq(SpaceInvite::getInviteeId, invitee.getId())
                .eq(SpaceInvite::getStatus, SpaceInviteStatus.PENDING)
                .eq(SpaceInvite::getIsDelete, 0));
        if (pendingCount != null && pendingCount > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "已有待处理邀请");
        }

        vipQuotaService.requireCanInviteMember(spaceId);

        SpaceInvite invite = new SpaceInvite();
        invite.setSpaceId(spaceId);
        invite.setInviterId(inviterId);
        invite.setInviteeId(invitee.getId());
        invite.setRole(request.getRole());
        invite.setStatus(SpaceInviteStatus.PENDING);
        int rows = spaceInviteMapper.insert(invite);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "邀请失败");
        }

        notificationService.create(
                invitee.getId(),
                inviterId,
                NotificationType.SPACE_INVITE,
                null,
                null,
                spaceId,
                space.getName()
        );

        return toInviteVO(invite, space, Map.of(
                inviterId, UserConverter.toVO(userMapper.selectById(inviterId)),
                invitee.getId(), UserConverter.toVO(invitee)
        ));
    }

    @Override
    public IPage<SpaceInviteVO> pageSpacePendingInvites(Long spaceId, Long operatorId, PageRequest pageRequest) {
        spaceService.requireCreator(spaceId, operatorId);
        spaceService.requireSpace(spaceId);
        return pageInvites(new LambdaQueryWrapper<SpaceInvite>()
                .eq(SpaceInvite::getSpaceId, spaceId)
                .eq(SpaceInvite::getStatus, SpaceInviteStatus.PENDING)
                .eq(SpaceInvite::getIsDelete, 0)
                .orderByDesc(SpaceInvite::getCreateTime), pageRequest);
    }

    @Override
    public IPage<SpaceInviteVO> pageMyPendingInvites(Long inviteeId, PageRequest pageRequest) {
        if (inviteeId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return pageInvites(new LambdaQueryWrapper<SpaceInvite>()
                .eq(SpaceInvite::getInviteeId, inviteeId)
                .eq(SpaceInvite::getStatus, SpaceInviteStatus.PENDING)
                .eq(SpaceInvite::getIsDelete, 0)
                .orderByDesc(SpaceInvite::getCreateTime), pageRequest);
    }

    @Override
    @Transactional
    public void accept(Long inviteId, Long userId) {
        SpaceInvite invite = requirePendingInvite(inviteId);
        if (!Objects.equals(invite.getInviteeId(), userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "只能处理发给自己的邀请");
        }
        spaceService.requireSpace(invite.getSpaceId());
        if (spaceService.isMember(invite.getSpaceId(), userId)) {
            invite.setStatus(SpaceInviteStatus.ACCEPTED);
            spaceInviteMapper.updateById(invite);
            return;
        }

        vipQuotaService.requireCanAcceptMember(invite.getSpaceId());

        SpaceMember member = new SpaceMember();
        member.setSpaceId(invite.getSpaceId());
        member.setUserId(userId);
        member.setRole(invite.getRole());
        int memberRows = spaceMemberMapper.insert(member);
        if (memberRows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "加入空间失败");
        }
        conversationLifecycleService.addMember(invite.getSpaceId(), userId);

        invite.setStatus(SpaceInviteStatus.ACCEPTED);
        int rows = spaceInviteMapper.updateById(invite);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "接受邀请失败");
        }
    }

    @Override
    @Transactional
    public void reject(Long inviteId, Long userId) {
        SpaceInvite invite = requirePendingInvite(inviteId);
        if (!Objects.equals(invite.getInviteeId(), userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "只能处理发给自己的邀请");
        }
        invite.setStatus(SpaceInviteStatus.REJECTED);
        int rows = spaceInviteMapper.updateById(invite);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "拒绝邀请失败");
        }
    }

    @Override
    @Transactional
    public void cancel(Long inviteId, Long operatorId) {
        SpaceInvite invite = requirePendingInvite(inviteId);
        if (!Objects.equals(invite.getInviterId(), operatorId)) {
            SpaceMember operator = spaceMemberMapper.selectOne(new LambdaQueryWrapper<SpaceMember>()
                    .eq(SpaceMember::getSpaceId, invite.getSpaceId())
                    .eq(SpaceMember::getUserId, operatorId)
                    .eq(SpaceMember::getIsDelete, 0)
                    .last("LIMIT 1"));
            if (operator == null || !SpaceRole.CREATOR.equals(operator.getRole())) {
                throw new BusinessException(ErrorCode.NO_AUTH, "仅邀请人或创建者可取消邀请");
            }
        }
        invite.setStatus(SpaceInviteStatus.CANCELLED);
        int rows = spaceInviteMapper.updateById(invite);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "取消邀请失败");
        }
    }

    private User resolveInvitee(SpaceInviteRequest request) {
        if (request.getUserId() != null) {
            User user = userMapper.selectById(request.getUserId());
            if (user == null || Objects.equals(user.getIsDelete(), 1)) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            return user;
        }
        if (StringUtils.hasText(request.getUserAccount())) {
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUserAccount, request.getUserAccount().trim())
                    .eq(User::getIsDelete, 0)
                    .last("LIMIT 1"));
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            return user;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "请提供 userId 或 userAccount");
    }

    private SpaceInvite requirePendingInvite(Long inviteId) {
        if (inviteId == null || inviteId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邀请 ID 不能为空");
        }
        SpaceInvite invite = spaceInviteMapper.selectById(inviteId);
        if (invite == null || Objects.equals(invite.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邀请不存在");
        }
        if (!SpaceInviteStatus.PENDING.equals(invite.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邀请已处理");
        }
        return invite;
    }

    private IPage<SpaceInviteVO> pageInvites(LambdaQueryWrapper<SpaceInvite> wrapper, PageRequest pageRequest) {
        PageRequest req = pageRequest != null ? pageRequest : new PageRequest();
        Page<SpaceInvite> page = new Page<>(req.getCurrent(), req.getPageSize());
        Page<SpaceInvite> result = spaceInviteMapper.selectPage(page, wrapper);
        return toVoPage(result);
    }

    private IPage<SpaceInviteVO> toVoPage(Page<SpaceInvite> result) {
        List<SpaceInvite> records = result.getRecords();
        if (records == null || records.isEmpty()) {
            IPage<SpaceInviteVO> empty = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
            empty.setRecords(List.of());
            return empty;
        }

        Set<Long> userIds = new HashSet<>();
        Set<Long> spaceIds = new HashSet<>();
        for (SpaceInvite invite : records) {
            userIds.add(invite.getInviterId());
            userIds.add(invite.getInviteeId());
            spaceIds.add(invite.getSpaceId());
        }

        Map<Long, UserVO> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, UserConverter::toVO, (a, b) -> a));
        Map<Long, Space> spaceMap = spaceIds.isEmpty()
                ? Collections.emptyMap()
                : spaceMapper.selectBatchIds(spaceIds).stream()
                .collect(Collectors.toMap(Space::getId, Function.identity(), (a, b) -> a));

        List<SpaceInviteVO> voList = records.stream()
                .map(invite -> toInviteVO(invite, spaceMap.get(invite.getSpaceId()), userMap))
                .toList();

        IPage<SpaceInviteVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    private SpaceInviteVO toInviteVO(SpaceInvite invite, Space space, Map<Long, UserVO> userMap) {
        SpaceInviteVO vo = new SpaceInviteVO();
        vo.setId(invite.getId());
        vo.setSpaceId(invite.getSpaceId());
        vo.setSpaceName(space != null ? space.getName() : null);
        vo.setRole(invite.getRole());
        vo.setStatus(invite.getStatus());
        vo.setCreateTime(invite.getCreateTime());
        vo.setInviter(userMap.get(invite.getInviterId()));
        vo.setInvitee(userMap.get(invite.getInviteeId()));
        return vo;
    }
}
