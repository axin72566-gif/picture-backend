package com.example.picturebackend.chat.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ConversationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String type;

    private Long spaceId;

    private String spaceName;

    private Long unreadCount;

    private ChatMessageVO lastMessage;

    private LocalDateTime updateTime;
}
