package com.example.picturebackend.space.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceChatEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_MESSAGE_NEW = "MESSAGE_NEW";

    public static final String TYPE_MESSAGE_DELETED = "MESSAGE_DELETED";

    private String type;

    private SpaceMessageVO message;

    private Long spaceId;

    private Long messageId;

    public static SpaceChatEvent messageNew(SpaceMessageVO message) {
        SpaceChatEvent event = new SpaceChatEvent();
        event.setType(TYPE_MESSAGE_NEW);
        event.setMessage(message);
        if (message != null) {
            event.setSpaceId(message.getSpaceId());
            event.setMessageId(message.getId());
        }
        return event;
    }

    public static SpaceChatEvent messageDeleted(Long spaceId, Long messageId) {
        SpaceChatEvent event = new SpaceChatEvent();
        event.setType(TYPE_MESSAGE_DELETED);
        event.setSpaceId(spaceId);
        event.setMessageId(messageId);
        return event;
    }
}
