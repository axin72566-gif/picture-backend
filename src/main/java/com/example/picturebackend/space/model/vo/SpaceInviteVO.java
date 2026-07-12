package com.example.picturebackend.space.model.vo;

import com.example.picturebackend.user.model.vo.UserVO;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SpaceInviteVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long spaceId;

    private String spaceName;

    private String role;

    private String status;

    private LocalDateTime createTime;

    private UserVO inviter;

    private UserVO invitee;
}
