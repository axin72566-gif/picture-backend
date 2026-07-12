package com.example.picturebackend.websocket;

import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.space.constant.SpaceChatConstant;
import com.example.picturebackend.space.constant.SpaceRole;
import com.example.picturebackend.space.service.SpaceService;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STOMP SUBSCRIBE 鉴权：仅空间成员可订阅 /topic/space.{id}。
 */
@Component
public class SpaceChatChannelInterceptor implements ChannelInterceptor {

    private static final Pattern SPACE_TOPIC_PATTERN = Pattern.compile("^/topic/space\\.(\\d+)$");

    private final SpaceService spaceService;

    public SpaceChatChannelInterceptor(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            Long userId = resolveUserId(accessor);
            if (userId == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN);
            }
            Long spaceId = parseSpaceId(accessor.getDestination());
            if (spaceId == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的订阅目的地");
            }
            spaceService.requireRoleAtLeast(spaceId, userId, SpaceRole.VIEWER);
        }
        return message;
    }

    @Nullable
    private static Long resolveUserId(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return null;
        }
        Object userId = sessionAttributes.get(SpaceChatConstant.WS_USER_ID_ATTR);
        if (userId instanceof Long longId) {
            return longId;
        }
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    @Nullable
    private static Long parseSpaceId(String destination) {
        if (destination == null) {
            return null;
        }
        Matcher matcher = SPACE_TOPIC_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
