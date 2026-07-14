package com.example.picturebackend.websocket;

import com.example.picturebackend.chat.constant.ChatConstant;
import com.example.picturebackend.interceptor.AuthTokenResolver;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手：校验 JWT，写入 session userId，并供后续 STOMP Principal 使用。
 */
@Component
public class SpaceChatHandshakeInterceptor implements HandshakeInterceptor {

    private final AuthTokenResolver authTokenResolver;

    public SpaceChatHandshakeInterceptor(AuthTokenResolver authTokenResolver) {
        this.authTokenResolver = authTokenResolver;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        String token = servletRequest.getServletRequest().getParameter("token");
        AuthTokenResolver.AuthContext context = authTokenResolver.resolveToken(token);
        if (context == null) {
            return false;
        }
        attributes.put(ChatConstant.WS_USER_ID_ATTR, context.userId());
        attributes.put(SpaceChatConstantCompat.WS_USER_ID_ATTR, context.userId());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }

    /** 兼容旧 SpaceChatConstant 键名 */
    private static final class SpaceChatConstantCompat {
        static final String WS_USER_ID_ATTR = "wsUserId";
    }
}
