package com.example.picturebackend.picture.model.vo;

import com.example.picturebackend.user.model.vo.UserVO;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PictureCommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long pictureId;

    private Long userId;

    private String content;

    private Long parentId;

    private Long rootId;

    private Long replyCount;

    private UserVO user;

    private LocalDateTime createTime;
}
