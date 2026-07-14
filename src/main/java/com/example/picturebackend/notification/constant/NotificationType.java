package com.example.picturebackend.notification.constant;

/**
 * 站内通知类型。
 */
public final class NotificationType {

    public static final String FOLLOW = "FOLLOW";

    public static final String COMMENT = "COMMENT";

    public static final String REPLY = "REPLY";

    public static final String LIKE = "LIKE";

    public static final String SPACE_INVITE = "SPACE_INVITE";

    /** 聊天中被 @ */
    public static final String CHAT_MENTION = "CHAT_MENTION";

    private NotificationType() {
    }
}
