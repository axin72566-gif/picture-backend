package com.example.picturebackend.chat.event;

import com.example.picturebackend.chat.constant.ChatConstant;
import com.example.picturebackend.chat.model.vo.ChatEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ChatEventPublisher.class);

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    public ChatEventPublisher(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(ChatEvent event) {
        if (event == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(event);
            stringRedisTemplate.convertAndSend(ChatConstant.REDIS_CHANNEL, json);
        } catch (Exception e) {
            log.warn("Publish chat event failed: {}", e.getMessage());
        }
    }
}
