package com.example.picturebackend.space.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SpaceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String description;

    private Long ownerId;

    /**
     * 当前用户在该空间的角色（列表/详情时填充）。
     */
    private String myRole;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
