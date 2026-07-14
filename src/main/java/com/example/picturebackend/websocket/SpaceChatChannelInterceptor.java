package com.example.picturebackend.websocket;

import com.example.picturebackend.chat.constant.ChatConstant;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.exception.BusinessException;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * CONNECT 时绑定 Principal；SUBSCRIBE 仅允许个人聊天队列。
 */
@Component
public class SpaceChatChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Long userId = resolveUserId(accessor);
            if (userId == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN);
            }
            accessor.setUser(new StompUserPrincipal(userId));
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            Long userId = resolveUserId(accessor);
            if (userId == null && accessor.getUser() instanceof StompUserPrincipal principal) {
                userId = principal.getUserId();
            }
            if (userId == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN);
            }
            String destination = accessor.getDestination();
            if (!isAllowedUserQueue(destination, userId)) {
                throw new BusinessException(ErrorCode.NO_AUTH, "无效的订阅目的地");
            }
        }
        return message;
    }

    private static boolean isAllowedUserQueue(String destination, Long userId) {
        if (destination == null || userId == null) {
            return false;
        }
        // 客户端订阅 /user/queue/chat，框架可能表现为 /user/{userId}/queue/chat 或 /user/queue/chat
        return destination.equals("/user" + ChatConstant.USER_QUEUE_CHAT)
                || destination.equals("/user/" + userId + ChatConstant.USER_QUEUE_CHAT)
                || destination.endsWith(ChatConstant.USER_QUEUE_CHAT);
    }

    @Nullable
    private static Long resolveUserId(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return null;
        }
        Object userId = sessionAttributes.get(ChatConstant.WS_USER_ID_ATTR);
        if (userId == null) {
            userId = sessionAttributes.get("wsUserId");
        }
        if (userId instanceof Long longId) {
            return longId;
        }
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }
}
