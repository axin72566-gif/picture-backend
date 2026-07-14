package com.example.picturebackend.chat.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChatReadRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long lastReadMessageId;
}
