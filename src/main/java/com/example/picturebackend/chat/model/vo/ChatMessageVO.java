package com.example.picturebackend.chat.model.vo;

import com.example.picturebackend.user.model.vo.UserVO;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatMessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long conversationId;

    private String messageType;

    private String content;

    private String mediaUrl;

    private Integer mediaWidth;

    private Integer mediaHeight;

    private Long mediaSize;

    private String mediaContentType;

    private LocalDateTime createTime;

    private UserVO sender;

    private ChatMessageReplyToVO replyTo;

    private List<UserVO> mentions;
}
