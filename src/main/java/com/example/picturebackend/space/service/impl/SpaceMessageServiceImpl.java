package com.example.picturebackend.space.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.picturebackend.chat.model.dto.ChatMessageAddRequest;
import com.example.picturebackend.chat.model.vo.ChatMessageReplyToVO;
import com.example.picturebackend.chat.model.vo.ChatMessageVO;
import com.example.picturebackend.chat.service.ChatService;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.space.model.dto.SpaceMessageAddRequest;
import com.example.picturebackend.space.model.vo.SpaceMessageReplyToVO;
import com.example.picturebackend.space.model.vo.SpaceMessageVO;
import com.example.picturebackend.space.service.SpaceMessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 兼容旧 /api/space/{id}/messages：委托到 ChatService。
 */
@Service
public class SpaceMessageServiceImpl implements SpaceMessageService {

    private final ChatService chatService;

    public SpaceMessageServiceImpl(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public IPage<SpaceMessageVO> pageMessages(Long spaceId, Long userId, PageRequest pageRequest) {
        Long conversationId = chatService.resolveSpaceConversationId(spaceId);
        IPage<ChatMessageVO> page = chatService.pageMessages(conversationId, userId, pageRequest);
        Page<SpaceMessageVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toSpaceVO).toList());
        return result;
    }

    @Override
    @Transactional
    public SpaceMessageVO sendMessage(Long spaceId, SpaceMessageAddRequest request, Long userId) {
        Long conversationId = chatService.resolveSpaceConversationId(spaceId);
        ChatMessageAddRequest chatRequest = new ChatMessageAddRequest();
        if (request != null) {
            chatRequest.setContent(request.getContent());
            chatRequest.setReplyToId(request.getReplyToId());
        }
        return toSpaceVO(chatService.sendMessage(conversationId, chatRequest, userId));
    }

    @Override
    @Transactional
    public void deleteMessage(Long spaceId, Long messageId, Long operatorId) {
        Long conversationId = chatService.resolveSpaceConversationId(spaceId);
        chatService.deleteMessage(conversationId, messageId, operatorId);
    }

    private SpaceMessageVO toSpaceVO(ChatMessageVO source) {
        if (source == null) {
            return null;
        }
        SpaceMessageVO vo = new SpaceMessageVO();
        vo.setId(source.getId());
        vo.setSpaceId(null);
        vo.setContent(source.getContent());
        vo.setCreateTime(source.getCreateTime());
        vo.setSender(source.getSender());
        if (source.getReplyTo() != null) {
            ChatMessageReplyToVO reply = source.getReplyTo();
            SpaceMessageReplyToVO replyTo = new SpaceMessageReplyToVO();
            replyTo.setId(reply.getId());
            replyTo.setContent(reply.getContent());
            replyTo.setDeleted(reply.getDeleted());
            replyTo.setSender(reply.getSender());
            vo.setReplyTo(replyTo);
        }
        return vo;
    }
}
