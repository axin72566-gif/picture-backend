package com.example.picturebackend.websocket;

import java.security.Principal;

/**
 * STOMP Principal，name = userId 字符串，供 convertAndSendToUser 使用。
 */
public class StompUserPrincipal implements Principal {

    private final Long userId;

    public StompUserPrincipal(Long userId) {
        this.userId = userId;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }

    public Long getUserId() {
        return userId;
    }
}
