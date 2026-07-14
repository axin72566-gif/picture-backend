package com.example.picturebackend.chat.model.vo;

import com.example.picturebackend.user.model.vo.UserVO;
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

    /** DM 对方用户；SPACE 时为 null */
    private UserVO peer;

    /** 展示标题：空间名或对方昵称 */
    private String title;

    private Long unreadCount;

    private ChatMessageVO lastMessage;

    private LocalDateTime updateTime;
}
