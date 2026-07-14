package com.example.picturebackend.chat.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChatEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_MESSAGE_NEW = "MESSAGE_NEW";

    public static final String TYPE_MESSAGE_DELETED = "MESSAGE_DELETED";

    public static final String TYPE_CONVERSATION_UPDATED = "CONVERSATION_UPDATED";

    public static final String TYPE_CONVERSATION_REMOVED = "CONVERSATION_REMOVED";

    private String type;

    private Long conversationId;

    private ChatMessageVO message;

    private Long messageId;

    private Long unreadCount;

    private ChatMessageVO lastMessage;

    /** 需要推送的用户（Redis 扇出用）；不下发到客户端 */
    private java.util.List<Long> targetUserIds;

    /** 仅推给单人时使用（已读更新等） */
    private Long targetUserId;

    public static ChatEvent messageNew(Long conversationId, ChatMessageVO message, java.util.List<Long> targetUserIds) {
        ChatEvent event = new ChatEvent();
        event.setType(TYPE_MESSAGE_NEW);
        event.setConversationId(conversationId);
        event.setMessage(message);
        event.setMessageId(message != null ? message.getId() : null);
        event.setTargetUserIds(targetUserIds);
        return event;
    }

    public static ChatEvent messageDeleted(Long conversationId, Long messageId, java.util.List<Long> targetUserIds) {
        ChatEvent event = new ChatEvent();
        event.setType(TYPE_MESSAGE_DELETED);
        event.setConversationId(conversationId);
        event.setMessageId(messageId);
        event.setTargetUserIds(targetUserIds);
        return event;
    }

    public static ChatEvent conversationUpdated(Long conversationId, Long unreadCount, ChatMessageVO lastMessage,
                                                Long targetUserId) {
        ChatEvent event = new ChatEvent();
        event.setType(TYPE_CONVERSATION_UPDATED);
        event.setConversationId(conversationId);
        event.setUnreadCount(unreadCount);
        event.setLastMessage(lastMessage);
        event.setTargetUserId(targetUserId);
        return event;
    }

    public static ChatEvent conversationRemoved(Long conversationId, Long targetUserId) {
        ChatEvent event = new ChatEvent();
        event.setType(TYPE_CONVERSATION_REMOVED);
        event.setConversationId(conversationId);
        event.setTargetUserId(targetUserId);
        return event;
    }
}
