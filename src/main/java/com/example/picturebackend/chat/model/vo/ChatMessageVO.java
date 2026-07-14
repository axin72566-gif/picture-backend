package com.example.picturebackend.chat.model.vo;

import com.example.picturebackend.user.model.vo.UserVO;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatMessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long conversationId;

    private String content;

    private LocalDateTime createTime;

    private UserVO sender;

    private ChatMessageReplyToVO replyTo;
}
