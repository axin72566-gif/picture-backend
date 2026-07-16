package com.example.picturebackend.coupon.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CouponActivityCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    private Integer discountCents;

    private Integer totalStock;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer couponValidDays;

    /** 创建后是否立即上架，默认 false */
    private Boolean online;
}
