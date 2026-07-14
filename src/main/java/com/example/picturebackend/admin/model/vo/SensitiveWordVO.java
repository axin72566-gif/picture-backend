package com.example.picturebackend.admin.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SensitiveWordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String word;

    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
