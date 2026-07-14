package com.example.picturebackend.chat.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ChatMessageAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String content;

    private Long replyToId;

    private String clientMsgId;

    /** 被 @ 的用户 ID 列表（服务端校验须为本会话成员） */
    private List<Long> mentionUserIds;
}
