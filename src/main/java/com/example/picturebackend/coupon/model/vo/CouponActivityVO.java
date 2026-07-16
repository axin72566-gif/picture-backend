package com.example.picturebackend.coupon.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CouponActivityVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private Integer discountCents;

    private Integer totalStock;

    private Integer remainStock;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer couponValidDays;

    private Integer status;

    /** 活动是否在进行中（上架且当前时间在窗口内） */
    private Boolean ongoing;

    /** 当前用户是否已领取 */
    private Boolean claimed;
}
