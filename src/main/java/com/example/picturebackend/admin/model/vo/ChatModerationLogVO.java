package com.example.picturebackend.admin.model.vo;

import com.example.picturebackend.user.model.vo.UserVO;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatModerationLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long conversationId;

    private Long senderId;

    private UserVO sender;

    private String messageType;

    private String originalContent;

    private String hitWords;

    private String action;

    private LocalDateTime createTime;
}
