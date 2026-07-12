package com.example.picturebackend.notification.model.vo;

import com.example.picturebackend.user.model.vo.UserVO;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class NotificationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String type;

    private Long pictureId;

    private Long commentId;

    private String content;

    private Integer isRead;

    private LocalDateTime createTime;

    private UserVO sender;
}
