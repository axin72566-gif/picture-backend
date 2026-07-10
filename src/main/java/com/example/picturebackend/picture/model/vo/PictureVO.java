package com.example.picturebackend.picture.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PictureVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String url;

    private String name;

    private Long size;

    private Integer width;

    private Integer height;

    private String contentType;

    private String format;

    private Long userId;

    private LocalDateTime createTime;
}
