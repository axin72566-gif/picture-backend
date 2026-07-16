package com.example.picturebackend.vip.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class VipOrderCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long planId;

    /** 可选：用户优惠券 ID，固定金额抵扣 */
    private Long couponId;
}
