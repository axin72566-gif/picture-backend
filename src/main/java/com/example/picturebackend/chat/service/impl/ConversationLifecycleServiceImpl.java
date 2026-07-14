package com.example.picturebackend.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.picturebackend.chat.constant.ConversationType;
import com.example.picturebackend.chat.entity.ChatMessage;
import com.example.picturebackend.chat.entity.Conversation;
import com.example.picturebackend.chat.entity.ConversationMember;
import com.example.picturebackend.chat.event.ChatEventPublisher;
import com.example.picturebackend.chat.mapper.ChatMessageMapper;
import com.example.picturebackend.chat.mapper.ConversationMapper;
import com.example.picturebackend.chat.mapper.ConversationMemberMapper;
import com.example.picturebackend.chat.model.vo.ChatEvent;
import com.example.picturebackend.chat.service.ConversationLifecycleService;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ConversationLifecycleServiceImpl implements ConversationLifecycleService {

    private final ConversationMapper conversationMapper;

    private final ConversationMemberMapper conversationMemberMapper;

    private final ChatMessageMapper chatMessageMapper;

    private final ChatEventPublisher chatEventPublisher;

    public ConversationLifecycleServiceImpl(ConversationMapper conversationMapper,
                                            ConversationMemberMapper conversationMemberMapper,
                                            ChatMessageMapper chatMessageMapper,
                                            ChatEventPublisher chatEventPublisher) {
        this.conversationMapper = conversationMapper;
        this.conversationMemberMapper = conversationMemberMapper;
        this.chatMessageMapper = chatMessageMapper;
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
