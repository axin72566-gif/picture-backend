package com.example.picturebackend.space.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceInviteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 被邀请人 ID；与 userAccount 二选一，都传时以 userId 为准。
     */
    private Long userId;

    /**
     * 被邀请人账号；与 userId 二选一。
     */
    private String userAccount;

    /**
     * 同意后角色：EDITOR / VIEWER。
     */
    private String role;
}
