package com.example.picturebackend.chat.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChatMessageAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String content;

    private Long replyToId;

    private String clientMsgId;
}
