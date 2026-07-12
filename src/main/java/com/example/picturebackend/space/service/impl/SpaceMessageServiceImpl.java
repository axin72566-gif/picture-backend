package com.example.picturebackend.space.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.space.constant.SpaceChatConstant;
import com.example.picturebackend.space.constant.SpaceRole;
import com.example.picturebackend.space.entity.SpaceMember;
import com.example.picturebackend.space.entity.SpaceMessage;
import com.example.picturebackend.space.mapper.SpaceMessageMapper;
import com.example.picturebackend.space.model.converter.SpaceMessageConverter;
import com.example.picturebackend.space.model.dto.SpaceMessageAddRequest;
import com.example.picturebackend.space.model.vo.SpaceChatEvent;
import com.example.picturebackend.space.model.vo.SpaceMessageReplyToVO;
import com.example.picturebackend.space.model.vo.SpaceMessageVO;
import com.example.picturebackend.space.service.SpaceMessageService;
import com.example.picturebackend.space.service.SpaceService;
import com.example.picturebackend.user.entity.User;
import com.example.picturebackend.user.mapper.UserMapper;
import com.example.picturebackend.user.model.converter.UserConverter;
import com.example.picturebackend.user.model.vo.UserVO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
public class SpaceMessageServiceImpl implements SpaceMessageService {

    private static final int MAX_CONTENT_LENGTH = 500;

    private final SpaceMessageMapper spaceMessageMapper;

    private final SpaceService spaceService;

    private final UserMapper userMapper;

    private final SimpMessagingTemplate messagingTemplate;

    public SpaceMessageServiceImpl(SpaceMessageMapper spaceMessageMapper,
                                   SpaceService spaceService,
                                   UserMapper userMapper,
                                   SimpMessagingTemplate messagingTemplate) {
        this.spaceMessageMapper = spaceMessageMapper;
        this.spaceService = spaceService;
        this.userMapper = userMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public IPage<SpaceMessageVO> pageMessages(Long spaceId, Long userId, PageRequest pageRequest) {
        spaceService.requireRoleAtLeast(spaceId, userId, SpaceRole.VIEWER);

        PageRequest req = pageRequest != null ? pageRequest : new PageRequest();
        Page<SpaceMessage> page = new Page<>(req.getCurrent(), req.getPageSize());
        Page<SpaceMessage> result = spaceMessageMapper.selectPage(page, new LambdaQueryWrapper<SpaceMessage>()
                .eq(SpaceMessage::getSpaceId, spaceId)
                .orderByDesc(SpaceMessage::getCreateTime));

        IPage<SpaceMessageVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(toVoList(result.getRecords()));
        return voPage;
    }

    @Override
    @Transactional
    public SpaceMessageVO sendMessage(Long spaceId, SpaceMessageAddRequest request, Long userId) {
        spaceService.requireRoleAtLeast(spaceId, userId, SpaceRole.VIEWER);
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能超过 500 个字符");
        }

        SpaceMessage replyTo = null;
        Long replyToId = request.getReplyToId();
        if (replyToId != null) {
            if (replyToId <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "回复消息 ID 无效");
            }
            replyTo = spaceMessageMapper.selectById(replyToId);
            if (replyTo == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "被回复的消息不存在");
            }
            if (!Objects.equals(replyTo.getSpaceId(), spaceId)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能回复其他空间的消息");
            }
        }

        SpaceMessage message = new SpaceMessage();
        message.setSpaceId(spaceId);
        message.setUserId(userId);
        message.setContent(content);
        message.setReplyToId(replyTo != null ? replyTo.getId() : null);

        int rows = spaceMessageMapper.insert(message);
        if (rows <= 0 || message.getId() == null) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "发送消息失败");
        }

        SpaceMessageVO vo = toVoList(List.of(message)).getFirst();
        messagingTemplate.convertAndSend(
                SpaceChatConstant.topicOf(spaceId),
                SpaceChatEvent.messageNew(vo));
        return vo;
    }

    @Override
    @Transactional
    public void deleteMessage(Long spaceId, Long messageId, Long operatorId) {
        if (messageId == null || messageId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息 ID 不能为空");
        }

        SpaceMember member = spaceService.requireRoleAtLeast(spaceId, operatorId, SpaceRole.VIEWER);
        SpaceMessage message = spaceMessageMapper.selectById(messageId);
        if (message == null || !Objects.equals(message.getSpaceId(), spaceId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息不存在");
        }

        boolean isAuthor = Objects.equals(message.getUserId(), operatorId);
        boolean isCreator = SpaceRole.CREATOR.equals(member.getRole());
        if (!isAuthor && !isCreator) {
            throw new BusinessException(ErrorCode.NO_AUTH, "只能删除自己的消息或由创建者删除");
        }

        int rows = spaceMessageMapper.deleteById(messageId);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "删除消息失败");
        }

        messagingTemplate.convertAndSend(
                SpaceChatConstant.topicOf(spaceId),
                SpaceChatEvent.messageDeleted(spaceId, messageId));
    }

    private List<SpaceMessageVO> toVoList(List<SpaceMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        Set<Long> userIds = new HashSet<>();
        Set<Long> replyToIds = new HashSet<>();
        for (SpaceMessage message : messages) {
            if (message.getUserId() != null) {
                userIds.add(message.getUserId());
            }
            if (message.getReplyToId() != null) {
                replyToIds.add(message.getReplyToId());
            }
        }

        Map<Long, SpaceMessage> replyMap = replyToIds.isEmpty()
                ? Collections.emptyMap()
                : spaceMessageMapper.selectBatchIdsIncludeDeleted(replyToIds).stream()
                .collect(Collectors.toMap(SpaceMessage::getId, Function.identity(), (a, b) -> a));

        for (SpaceMessage reply : replyMap.values()) {
            if (reply.getUserId() != null) {
                userIds.add(reply.getUserId());
            }
        }

        Map<Long, UserVO> userVOMap = loadUserVOMap(userIds);

        return messages.stream().map(message -> {
            SpaceMessageVO vo = SpaceMessageConverter.toVO(message);
            vo.setSender(userVOMap.get(message.getUserId()));
            if (message.getReplyToId() != null) {
                vo.setReplyTo(buildReplyToVO(replyMap.get(message.getReplyToId()), userVOMap));
            }
            return vo;
        }).toList();
    }

    private SpaceMessageReplyToVO buildReplyToVO(SpaceMessage reply, Map<Long, UserVO> userVOMap) {
        SpaceMessageReplyToVO replyToVO = new SpaceMessageReplyToVO();
        if (reply == null) {
            replyToVO.setDeleted(true);
            replyToVO.setContent(null);
            return replyToVO;
        }
        replyToVO.setId(reply.getId());
        boolean deleted = Objects.equals(reply.getIsDelete(), 1);
        replyToVO.setDeleted(deleted);
        replyToVO.setContent(deleted ? null : reply.getContent());
        replyToVO.setSender(userVOMap.get(reply.getUserId()));
        return replyToVO;
    }

    private Map<Long, UserVO> loadUserVOMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(User::getId, UserConverter::toVO, (a, b) -> a));
    }
}
