package com.example.picturebackend.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.picturebackend.chat.constant.ConversationType;
import com.example.picturebackend.chat.entity.ChatMessage;
import com.example.picturebackend.chat.entity.Conversation;
import com.example.picturebackend.chat.entity.ConversationDmPair;
import com.example.picturebackend.chat.entity.ConversationMember;
import com.example.picturebackend.chat.event.ChatEventPublisher;
import com.example.picturebackend.chat.mapper.ChatMessageMapper;
import com.example.picturebackend.chat.mapper.ConversationDmPairMapper;
import com.example.picturebackend.chat.mapper.ConversationMapper;
import com.example.picturebackend.chat.mapper.ConversationMemberMapper;
import com.example.picturebackend.chat.model.vo.ChatEvent;
import com.example.picturebackend.chat.service.ConversationLifecycleService;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.user.entity.User;
import com.example.picturebackend.user.mapper.UserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ConversationLifecycleServiceImpl implements ConversationLifecycleService {

    private final ConversationMapper conversationMapper;

    private final ConversationMemberMapper conversationMemberMapper;

    private final ChatMessageMapper chatMessageMapper;

    private final ConversationDmPairMapper conversationDmPairMapper;

    private final UserMapper userMapper;

    private final ChatEventPublisher chatEventPublisher;

    public ConversationLifecycleServiceImpl(ConversationMapper conversationMapper,
                                            ConversationMemberMapper conversationMemberMapper,
                                            ChatMessageMapper chatMessageMapper,
                                            ConversationDmPairMapper conversationDmPairMapper,
                                            UserMapper userMapper,
                                            ChatEventPublisher chatEventPublisher) {
        this.conversationMapper = conversationMapper;
        this.conversationMemberMapper = conversationMemberMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.conversationDmPairMapper = conversationDmPairMapper;
        this.userMapper = userMapper;
        this.chatEventPublisher = chatEventPublisher;
    }

    @Override
    @Transactional
    public Conversation createSpaceConversation(Long spaceId, Long creatorUserId) {
        if (spaceId == null || creatorUserId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Conversation existing = findSpaceConversation(spaceId);
        if (existing != null) {
            addMemberInternal(existing.getId(), creatorUserId);
            return existing;
        }
        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.SPACE);
        conversation.setSpaceId(spaceId);
        int rows = conversationMapper.insert(conversation);
        if (rows <= 0 || conversation.getId() == null) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "创建会话失败");
        }
        addMemberInternal(conversation.getId(), creatorUserId);
        return conversation;
    }

    @Override
    @Transactional
    public void addMember(Long spaceId, Long userId) {
        Conversation conversation = requireSpaceConversation(spaceId);
        addMemberInternal(conversation.getId(), userId);
    }

    @Override
    @Transactional
    public void removeMember(Long spaceId, Long userId) {
        Conversation conversation = findSpaceConversation(spaceId);
        if (conversation == null) {
            return;
        }
        conversationMemberMapper.deletePhysically(conversation.getId(), userId);
        chatEventPublisher.publish(ChatEvent.conversationRemoved(conversation.getId(), userId));
    }

    @Override
    @Transactional
    public void dissolveSpaceConversation(Long spaceId) {
        Conversation conversation = findSpaceConversation(spaceId);
        if (conversation == null) {
            return;
        }
        conversationMapper.deleteById(conversation.getId());
        conversationMemberMapper.deleteAllByConversationIdPhysically(conversation.getId());
        chatMessageMapper.delete(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversation.getId()));
    }

    @Override
    @Transactional
    public DmOpenResult openOrGetDm(Long userId, Long peerUserId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        if (peerUserId == null || peerUserId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "对方用户 ID 不能为空");
        }
        if (userId.equals(peerUserId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能与自己私聊");
        }
        User peer = userMapper.selectById(peerUserId);
        if (peer == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "对方用户不存在");
        }

        long low = Math.min(userId, peerUserId);
        long high = Math.max(userId, peerUserId);
        ConversationDmPair pair = conversationDmPairMapper.selectOne(new LambdaQueryWrapper<ConversationDmPair>()
                .eq(ConversationDmPair::getUserLowId, low)
                .eq(ConversationDmPair::getUserHighId, high)
                .last("LIMIT 1"));
        if (pair != null) {
            Conversation conversation = conversationMapper.selectById(pair.getConversationId());
            if (conversation == null) {
                throw new BusinessException(ErrorCode.SERVER_ERROR, "私聊会话数据异常");
            }
            return new DmOpenResult(conversation, false);
        }

        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.DM);
        conversation.setSpaceId(null);
        int rows = conversationMapper.insert(conversation);
        if (rows <= 0 || conversation.getId() == null) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "创建私聊失败");
        }
        addMemberInternal(conversation.getId(), userId);
        addMemberInternal(conversation.getId(), peerUserId);

        ConversationDmPair created = new ConversationDmPair();
        created.setUserLowId(low);
        created.setUserHighId(high);
        created.setConversationId(conversation.getId());
        try {
            conversationDmPairMapper.insert(created);
        } catch (DuplicateKeyException e) {
            // 并发下另一请求已创建：清掉本事务刚插入的会话/成员，回查已有会话
            conversationMemberMapper.deleteAllByConversationIdPhysically(conversation.getId());
            conversationMapper.deleteById(conversation.getId());
            ConversationDmPair raced = conversationDmPairMapper.selectOne(new LambdaQueryWrapper<ConversationDmPair>()
                    .eq(ConversationDmPair::getUserLowId, low)
                    .eq(ConversationDmPair::getUserHighId, high)
                    .last("LIMIT 1"));
            if (raced != null) {
                Conversation existing = conversationMapper.selectById(raced.getConversationId());
                if (existing != null) {
                    return new DmOpenResult(existing, false);
                }
            }
            throw new BusinessException(ErrorCode.SERVER_ERROR, "创建私聊失败");
        }
        return new DmOpenResult(conversation, true);
    }

    private void addMemberInternal(Long conversationId, Long userId) {
        Long count = conversationMemberMapper.selectCount(new LambdaQueryWrapper<ConversationMember>()
                .eq(ConversationMember::getConversationId, conversationId)
                .eq(ConversationMember::getUserId, userId));
        if (count != null && count > 0) {
            return;
        }
        ConversationMember member = new ConversationMember();
        member.setConversationId(conversationId);
        member.setUserId(userId);
        member.setLastReadMessageId(0L);
        member.setJoinedAt(LocalDateTime.now());
        conversationMemberMapper.insert(member);
    }

    private Conversation requireSpaceConversation(Long spaceId) {
        Conversation conversation = findSpaceConversation(spaceId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话不存在");
        }
        return conversation;
    }

    private Conversation findSpaceConversation(Long spaceId) {
        return conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getType, ConversationType.SPACE)
                .eq(Conversation::getSpaceId, spaceId)
                .last("LIMIT 1"));
    }
}
