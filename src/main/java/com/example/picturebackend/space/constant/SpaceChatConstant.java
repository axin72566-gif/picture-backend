package com.example.picturebackend.space.constant;

public final class SpaceChatConstant {

    public static final String WS_USER_ID_ATTR = "wsUserId";

    public static final String TOPIC_PREFIX = "/topic/space.";

    private SpaceChatConstant() {
    }

    public static String topicOf(Long spaceId) {
        return TOPIC_PREFIX + spaceId;
    }
}
