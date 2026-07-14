package com.example.picturebackend.chat.service.impl;

import com.example.picturebackend.chat.entity.ChatModerationLog;
import com.example.picturebackend.chat.mapper.ChatModerationLogMapper;
import com.example.picturebackend.chat.service.ChatModerationLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatModerationLogServiceImpl implements ChatModerationLogService {

    public static final String ACTION_BLOCK = "BLOCK";

    private static final int MAX_CONTENT_STORE = 500;

    private final ChatModerationLogMapper chatModerationLogMapper;

    public ChatModerationLogServiceImpl(ChatModerationLogMapper chatModerationLogMapper) {
        this.chatModerationLogMapper = chatModerationLogMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveBlockLog(Long conversationId, Long senderId, String messageType, String content,
                             List<String> hits) {
        ChatModerationLog logRow = new ChatModerationLog();
        logRow.setConversationId(conversationId);
        logRow.setSenderId(senderId);
        logRow.setMessageType(messageType);
        logRow.setOriginalContent(truncate(content));
        logRow.setHitWords(String.join(",", hits));
        logRow.setAction(ACTION_BLOCK);
        chatModerationLogMapper.insert(logRow);
    }

    private static String truncate(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.trim();
        if (trimmed.length() <= MAX_CONTENT_STORE) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_CONTENT_STORE);
    }
}
