package com.example.picturebackend.vip.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class VipPlanVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String code;

    private String name;

    private Integer durationDays;

    private Integer priceCents;
}
