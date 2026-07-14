package com.example.picturebackend.chat.config;

import com.example.picturebackend.chat.constant.ChatConstant;
import com.example.picturebackend.chat.event.ChatEventFanoutService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

@Configuration
public class ChatRedisConfig {

    @Bean
    public RedisMessageListenerContainer chatRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ChatEventFanoutService fanoutService) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        MessageListener listener = (Message message, byte[] pattern) -> {
            if (message == null || message.getBody() == null) {
                return;
            }
            fanoutService.deliver(new String(message.getBody(), StandardCharsets.UTF_8));
        };
        container.addMessageListener(listener, new ChannelTopic(ChatConstant.REDIS_CHANNEL));
        return container;
    }
}
