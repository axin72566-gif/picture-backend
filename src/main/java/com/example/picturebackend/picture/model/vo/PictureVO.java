package com.example.picturebackend.picture.model.vo;

import com.example.picturebackend.user.model.vo.UserVO;
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

    private String description;

    private Long userId;

    /**
     * 所属空间 ID；null 表示个人图。
     */
    private Long spaceId;

    private UserVO user;

    private Long likeCount;

    private Boolean liked;

    private LocalDateTime createTime;
}
