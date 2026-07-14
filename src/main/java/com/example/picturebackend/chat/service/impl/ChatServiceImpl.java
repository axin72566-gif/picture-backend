package com.example.picturebackend.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.picturebackend.chat.constant.ConversationType;
import com.example.picturebackend.chat.entity.ChatMessage;
import com.example.picturebackend.chat.entity.Conversation;
import com.example.picturebackend.chat.entity.ConversationMember;
import com.example.picturebackend.chat.event.ChatEventPublisher;
import com.example.picturebackend.chat.mapper.ChatMessageMapper;
import com.example.picturebackend.chat.mapper.ConversationMapper;
import com.example.picturebackend.chat.mapper.ConversationMemberMapper;
import com.example.picturebackend.chat.model.converter.ChatMessageConverter;
import com.example.picturebackend.chat.model.dto.ChatMessageAddRequest;
import com.example.picturebackend.chat.model.dto.ChatReadRequest;
import com.example.picturebackend.chat.model.vo.ChatEvent;
import com.example.picturebackend.chat.model.vo.ChatMessageReplyToVO;
import com.example.picturebackend.chat.model.vo.ChatMessageVO;
import com.example.picturebackend.chat.model.vo.ConversationVO;
import com.example.picturebackend.chat.service.ChatService;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.space.constant.SpaceRole;
import com.example.picturebackend.space.entity.Space;
import com.example.picturebackend.space.entity.SpaceMember;
import com.example.picturebackend.space.mapper.SpaceMapper;
import com.example.picturebackend.space.service.SpaceService;
import com.example.picturebackend.user.entity.User;
import com.example.picturebackend.user.mapper.UserMapper;
import com.example.picturebackend.user.model.converter.UserConverter;
import com.example.picturebackend.user.model.vo.UserVO;
import org.springframework.dao.DuplicateKeyException;
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
public class ChatServiceImpl implements ChatService {

    private static final int MAX_CONTENT_LENGTH = 500;

    private static final int DEFAULT_SINCE_LIMIT = 100;

    private final ConversationMapper conversationMapper;

    private final ConversationMemberMapper conversationMemberMapper;

    private final ChatMessageMapper chatMessageMapper;

    private final SpaceMapper spaceMapper;

    private final UserMapper userMapper;

    private final SpaceService spaceService;

    private final ChatEventPublisher chatEventPublisher;

    public ChatServiceImpl(ConversationMapper conversationMapper,
                           ConversationMemberMapper conversationMemberMapper,
                           ChatMessageMapper chatMessageMapper,
                           SpaceMapper spaceMapper,
                           UserMapper userMapper,
                           SpaceService spaceService,
                           ChatEventPublisher chatEventPublisher) {
        this.conversationMapper = conversationMapper;
        this.conversationMemberMapper = conversationMemberMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.spaceMapper = spaceMapper;
        this.userMapper = userMapper;
        this.spaceService = spaceService;
        this.chatEventPublisher = chatEventPublisher;
    }

    @Override
    public IPage<ConversationVO> pageConversations(Long userId, PageRequest pageRequest) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        PageRequest req = pageRequest != null ? pageRequest : new PageRequest();
        Page<ConversationMember> page = new Page<>(req.getCurrent(), req.getPageSize());
        Page<ConversationMember> memberPage = conversationMemberMapper.selectPage(page,
                new LambdaQueryWrapper<ConversationMember>()
                        .eq(ConversationMember::getUserId, userId)
                        .orderByDesc(ConversationMember::getJoinedAt));

        List<ConversationMember> members = memberPage.getRecords();
        IPage<ConversationVO> result = new Page<>(memberPage.getCurrent(), memberPage.getSize(), memberPage.getTotal());
        if (members == null || members.isEmpty()) {
            result.setRecords(List.of());
            return result;
        }

        Set<Long> conversationIds = members.stream().map(ConversationMember::getConversationId).collect(Collectors.toSet());
        Map<Long, Conversation> conversationMap = conversationMapper.selectBatchIds(conversationIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Conversation::getId, Function.identity(), (a, b) -> a));

        Set<Long> spaceIds = conversationMap.values().stream()
                .map(Conversation::getSpaceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Space> spaceMap = spaceIds.isEmpty()
                ? Collections.emptyMap()
                : spaceMapper.selectBatchIds(spaceIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Space::getId, Function.identity(), (a, b) -> a));

        Map<Long, ConversationMember> memberMap = members.stream()
                .collect(Collectors.toMap(ConversationMember::getConversationId, Function.identity(), (a, b) -> a));

        List<ConversationVO> vos = conversationIds.stream()
                .map(conversationMap::get)
                .filter(Objects::nonNull)
                .map(c -> toConversationVO(c, memberMap.get(c.getId()), spaceMap, userId))
                .sorted((a, b) -> {
                    var ta = a.getLastMessage() != null ? a.getLastMessage().getCreateTime() : a.getUpdateTime();
                    var tb = b.getLastMessage() != null ? b.getLastMessage().getCreateTime() : b.getUpdateTime();
                    if (ta == null && tb == null) return 0;
                    if (ta == null) return 1;
                    if (tb == null) return -1;
                    return tb.compareTo(ta);
                })
                .toList();
        result.setRecords(vos);
        return result;
    }

    @Override
    public ConversationVO getSpaceConversation(Long spaceId, Long userId) {
        spaceService.requireRoleAtLeast(spaceId, userId, SpaceRole.VIEWER);
        Conversation conversation = requireSpaceConversationEntity(spaceId);
        ConversationMember member = requireMember(conversation.getId(), userId);
        Map<Long, Space> spaceMap = Map.of(spaceId, spaceService.requireSpace(spaceId));
        return toConversationVO(conversation, member, spaceMap, userId);
    }

    @Override
    public IPage<ChatMessageVO> pageMessages(Long conversationId, Long userId, PageRequest pageRequest) {
        requireMember(conversationId, userId);
        PageRequest req = pageRequest != null ? pageRequest : new PageRequest();
        Page<ChatMessage> page = new Page<>(req.getCurrent(), req.getPageSize());
        Page<ChatMessage> result = chatMessageMapper.selectPage(page, new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByDesc(ChatMessage::getId));
        IPage<ChatMessageVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(toVoList(result.getRecords()));
        return voPage;
    }

    @Override
    public List<ChatMessageVO> listMessagesSince(Long conversationId, Long userId, Long sinceId, int limit) {
        requireMember(conversationId, userId);
        if (sinceId == null || sinceId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "sinceId 无效");
        }
        int size = limit > 0 ? Math.min(limit, 200) : DEFAULT_SINCE_LIMIT;
        List<ChatMessage> messages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .gt(ChatMessage::getId, sinceId)
                .orderByAsc(ChatMessage::getId)
                .last("LIMIT " + size));
        return toVoList(messages);
    }

    @Override
    @Transactional
    public ChatMessageVO sendMessage(Long conversationId, ChatMessageAddRequest request, Long userId) {
        requireMember(conversationId, userId);
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

        String clientMsgId = StringUtils.hasText(request.getClientMsgId()) ? request.getClientMsgId().trim() : null;
        if (clientMsgId != null) {
            ChatMessage existing = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSenderId, userId)
                    .eq(ChatMessage::getClientMsgId, clientMsgId)
                    .last("LIMIT 1"));
            if (existing != null) {
                return toVoList(List.of(existing)).getFirst();
            }
        }

        ChatMessage replyTo = null;
        Long replyToId = request.getReplyToId();
        if (replyToId != null) {
            if (replyToId <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "回复消息 ID 无效");
            }
            replyTo = chatMessageMapper.selectById(replyToId);
            if (replyTo == null || !Objects.equals(replyTo.getConversationId(), conversationId)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "被回复的消息不存在");
            }
        }

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setContent(content);
        message.setReplyToId(replyTo != null ? replyTo.getId() : null);
        message.setClientMsgId(clientMsgId);

        try {
            int rows = chatMessageMapper.insert(message);
            if (rows <= 0 || message.getId() == null) {
                throw new BusinessException(ErrorCode.SERVER_ERROR, "发送消息失败");
            }
        } catch (DuplicateKeyException e) {
            ChatMessage existing = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSenderId, userId)
                    .eq(ChatMessage::getClientMsgId, clientMsgId)
                    .last("LIMIT 1"));
            if (existing != null) {
                return toVoList(List.of(existing)).getFirst();
            }
            throw new BusinessException(ErrorCode.SERVER_ERROR, "发送消息失败");
        }

        ChatMessageVO vo = toVoList(List.of(message)).getFirst();
        List<Long> targets = conversationMemberMapper.selectUserIdsByConversationId(conversationId);
        chatEventPublisher.publish(ChatEvent.messageNew(conversationId, vo, targets));

        // 同步更新其他成员未读感知：给除发送者外每人推 CONVERSATION_UPDATED
        for (Long targetId : targets) {
            if (Objects.equals(targetId, userId)) {
                continue;
            }
            ConversationMember member = conversationMemberMapper.selectOne(new LambdaQueryWrapper<ConversationMember>()
                    .eq(ConversationMember::getConversationId, conversationId)
                    .eq(ConversationMember::getUserId, targetId)
                    .last("LIMIT 1"));
            long unread = member == null ? 0
                    : chatMessageMapper.countUnread(conversationId,
                    member.getLastReadMessageId() == null ? 0L : member.getLastReadMessageId(),
                    targetId);
            chatEventPublisher.publish(ChatEvent.conversationUpdated(conversationId, unread, vo, targetId));
        }
        return vo;
    }

    @Override
    @Transactional
    public void deleteMessage(Long conversationId, Long messageId, Long operatorId) {
        if (messageId == null || messageId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息 ID 不能为空");
        }
        ConversationMember member = requireMember(conversationId, operatorId);
        ChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null || !Objects.equals(message.getConversationId(), conversationId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息不存在");
        }

        boolean isAuthor = Objects.equals(message.getSenderId(), operatorId);
        boolean isCreator = false;
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null && ConversationType.SPACE.equals(conversation.getType())
                && conversation.getSpaceId() != null) {
            SpaceMember spaceMember = spaceService.requireMember(conversation.getSpaceId(), operatorId);
            isCreator = SpaceRole.CREATOR.equals(spaceMember.getRole());
        }
        if (!isAuthor && !isCreator) {
            throw new BusinessException(ErrorCode.NO_AUTH, "只能删除自己的消息或由创建者删除");
        }

        int rows = chatMessageMapper.deleteById(messageId);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "删除消息失败");
        }
        List<Long> targets = conversationMemberMapper.selectUserIdsByConversationId(conversationId);
        chatEventPublisher.publish(ChatEvent.messageDeleted(conversationId, messageId, targets));
    }

    @Override
    @Transactional
    public void markRead(Long conversationId, ChatReadRequest request, Long userId) {
        ConversationMember member = requireMember(conversationId, userId);
        if (request == null || request.getLastReadMessageId() == null || request.getLastReadMessageId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "lastReadMessageId 无效");
        }
        long current = member.getLastReadMessageId() == null ? 0L : member.getLastReadMessageId();
        long next = request.getLastReadMessageId();
        if (next <= current) {
            return;
        }
        member.setLastReadMessageId(next);
        conversationMemberMapper.updateById(member);

        ChatMessage last = chatMessageMapper.selectById(next);
        ChatMessageVO lastVo = last != null && Objects.equals(last.getConversationId(), conversationId)
                ? toVoList(List.of(last)).getFirst()
                : null;
        chatEventPublisher.publish(ChatEvent.conversationUpdated(conversationId, 0L, lastVo, userId));
    }

    @Override
    public Long resolveSpaceConversationId(Long spaceId) {
        Conversation conversation = requireSpaceConversationEntity(spaceId);
        return conversation.getId();
    }

    private ConversationVO toConversationVO(Conversation conversation,
                                            ConversationMember member,
                                            Map<Long, Space> spaceMap,
                                            Long userId) {
        ConversationVO vo = new ConversationVO();
        vo.setId(conversation.getId());
        vo.setType(conversation.getType());
        vo.setSpaceId(conversation.getSpaceId());
        vo.setUpdateTime(conversation.getUpdateTime());
        if (conversation.getSpaceId() != null) {
            Space space = spaceMap.get(conversation.getSpaceId());
            if (space != null) {
                vo.setSpaceName(space.getName());
            }
        }
        long lastRead = member != null && member.getLastReadMessageId() != null
                ? member.getLastReadMessageId() : 0L;
        vo.setUnreadCount(chatMessageMapper.countUnread(conversation.getId(), lastRead, userId));

        ChatMessage last = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversation.getId())
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT 1"));
        if (last != null) {
            List<ChatMessageVO> list = toVoList(List.of(last));
            vo.setLastMessage(list.isEmpty() ? null : list.getFirst());
        }
        return vo;
    }

    private ConversationMember requireMember(Long conversationId, Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        if (conversationId == null || conversationId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话 ID 不能为空");
        }
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话不存在");
        }
        ConversationMember member = conversationMemberMapper.selectOne(new LambdaQueryWrapper<ConversationMember>()
                .eq(ConversationMember::getConversationId, conversationId)
                .eq(ConversationMember::getUserId, userId)
                .last("LIMIT 1"));
        if (member == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, "不是该会话成员");
        }
        if (ConversationType.SPACE.equals(conversation.getType()) && conversation.getSpaceId() != null) {
            spaceService.requireRoleAtLeast(conversation.getSpaceId(), userId, SpaceRole.VIEWER);
        }
        return member;
    }

    private Conversation requireSpaceConversationEntity(Long spaceId) {
        Conversation conversation = conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getType, ConversationType.SPACE)
                .eq(Conversation::getSpaceId, spaceId)
                .last("LIMIT 1"));
        if (conversation == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话不存在");
        }
        return conversation;
    }

    private List<ChatMessageVO> toVoList(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = new HashSet<>();
        Set<Long> replyToIds = new HashSet<>();
        for (ChatMessage message : messages) {
            if (message.getSenderId() != null) {
                userIds.add(message.getSenderId());
            }
            if (message.getReplyToId() != null) {
                replyToIds.add(message.getReplyToId());
            }
        }
        Map<Long, ChatMessage> replyMap = replyToIds.isEmpty()
                ? Collections.emptyMap()
                : chatMessageMapper.selectBatchIdsIncludeDeleted(replyToIds).stream()
                .collect(Collectors.toMap(ChatMessage::getId, Function.identity(), (a, b) -> a));
        for (ChatMessage reply : replyMap.values()) {
            if (reply.getSenderId() != null) {
                userIds.add(reply.getSenderId());
            }
        }
        Map<Long, UserVO> userVOMap = loadUserVOMap(userIds);
        return messages.stream().map(message -> {
            ChatMessageVO vo = ChatMessageConverter.toVO(message);
            vo.setSender(userVOMap.get(message.getSenderId()));
            if (message.getReplyToId() != null) {
                vo.setReplyTo(buildReplyToVO(replyMap.get(message.getReplyToId()), userVOMap));
            }
            return vo;
        }).toList();
    }

    private ChatMessageReplyToVO buildReplyToVO(ChatMessage reply, Map<Long, UserVO> userVOMap) {
        ChatMessageReplyToVO replyToVO = new ChatMessageReplyToVO();
        if (reply == null) {
            replyToVO.setDeleted(true);
            return replyToVO;
        }
        replyToVO.setId(reply.getId());
        boolean deleted = Objects.equals(reply.getIsDelete(), 1);
        replyToVO.setDeleted(deleted);
        replyToVO.setContent(deleted ? null : reply.getContent());
        replyToVO.setSender(userVOMap.get(reply.getSenderId()));
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
