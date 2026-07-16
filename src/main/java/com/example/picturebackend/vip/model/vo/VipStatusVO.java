package com.example.picturebackend.vip.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class VipStatusVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean vipActive;

    private LocalDateTime vipExpireTime;

    private Integer maxOwnedSpaces;

    private Integer ownedSpaceCount;

    private Integer maxMembersPerSpace;
}
