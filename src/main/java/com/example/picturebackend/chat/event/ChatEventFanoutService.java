package com.example.picturebackend.chat.event;

import com.example.picturebackend.chat.constant.ChatConstant;
import com.example.picturebackend.chat.model.vo.ChatEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class ChatEventFanoutService {

    private static final Logger log = LoggerFactory.getLogger(ChatEventFanoutService.class);

    private final SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper;

    public ChatEventFanoutService(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    public void deliver(String json) {
        try {
            ChatEvent event = objectMapper.readValue(json, ChatEvent.class);
            deliver(event);
        } catch (Exception e) {
            log.warn("Parse chat event failed: {}", e.getMessage());
        }
    }

    public void deliver(ChatEvent event) {
        if (event == null) {
            return;
        }
        ChatEvent clientEvent = stripTargets(event);
        Set<Long> targets = new LinkedHashSet<>();
        if (event.getTargetUserId() != null) {
            targets.add(event.getTargetUserId());
        }
        if (event.getTargetUserIds() != null) {
            targets.addAll(event.getTargetUserIds());
        }
        for (Long userId : targets) {
            if (userId == null) {
                continue;
            }
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    ChatConstant.USER_QUEUE_CHAT,
                    clientEvent);
        }
    }

    private static ChatEvent stripTargets(ChatEvent source) {
        ChatEvent copy = new ChatEvent();
        copy.setType(source.getType());
        copy.setConversationId(source.getConversationId());
        copy.setMessage(source.getMessage());
        copy.setMessageId(source.getMessageId());
        copy.setUnreadCount(source.getUnreadCount());
        copy.setLastMessage(source.getLastMessage());
        return copy;
    }
}
