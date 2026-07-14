package com.example.picturebackend.chat.model.vo;

import com.example.picturebackend.user.model.vo.UserVO;
import lombok.Data;

import java.io.Serializable;

@Data
public class ChatMessageReplyToVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String messageType;

    private String content;

    private Boolean deleted;

    private UserVO sender;
}
