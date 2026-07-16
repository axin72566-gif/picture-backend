package com.example.picturebackend.coupon.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserCouponVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long activityId;

    private String activityName;

    private Integer discountCents;

    private String status;

    private LocalDateTime expireTime;

    private String lockOrderNo;

    private LocalDateTime createTime;
}
