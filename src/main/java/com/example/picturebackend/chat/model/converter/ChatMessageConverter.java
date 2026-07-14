package com.example.picturebackend.chat.model.converter;

import com.example.picturebackend.chat.entity.ChatMessage;
import com.example.picturebackend.chat.model.vo.ChatMessageVO;

public final class ChatMessageConverter {

    private ChatMessageConverter() {
    }

    public static ChatMessageVO toVO(ChatMessage message) {
        if (message == null) {
            return null;
        }
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setConversationId(message.getConversationId());
        vo.setContent(message.getContent());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
