package com.example.picturebackend.vip.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class VipOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String orderNo;

    private Long planId;

    private String planCode;

    private String planName;

    private Integer durationDays;

    private Integer originalAmountCents;

    private Integer discountCents;

    private Long couponId;

    private Integer amountCents;

    private String status;

    private LocalDateTime expireTime;

    private LocalDateTime paidTime;

    private LocalDateTime createTime;
}
