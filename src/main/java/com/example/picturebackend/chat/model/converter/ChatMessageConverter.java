package com.example.picturebackend.chat.model.converter;

import com.example.picturebackend.chat.constant.ChatMessageType;
import com.example.picturebackend.chat.entity.ChatMessage;
import com.example.picturebackend.chat.model.vo.ChatMessageVO;
import org.springframework.util.StringUtils;

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
        vo.setMessageType(StringUtils.hasText(message.getMessageType())
                ? message.getMessageType() : ChatMessageType.TEXT);
        vo.setContent(message.getContent());
        vo.setMediaUrl(message.getMediaUrl());
        vo.setMediaWidth(message.getMediaWidth());
        vo.setMediaHeight(message.getMediaHeight());
        vo.setMediaSize(message.getMediaSize());
        vo.setMediaContentType(message.getMediaContentType());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    /** 会话列表预览文案 */
    public static String previewText(ChatMessage message) {
        if (message == null) {
            return null;
        }
        String type = StringUtils.hasText(message.getMessageType())
                ? message.getMessageType() : ChatMessageType.TEXT;
        if (ChatMessageType.IMAGE.equals(type)) {
            if (StringUtils.hasText(message.getContent())) {
                return message.getContent().trim();
            }
            return "[图片]";
        }
        return message.getContent();
    }
}
