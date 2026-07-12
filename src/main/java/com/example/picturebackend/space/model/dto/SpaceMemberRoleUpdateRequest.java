package com.example.picturebackend.space.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceMemberRoleUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 目标角色：EDITOR / VIEWER。
     */
    private String role;
}
