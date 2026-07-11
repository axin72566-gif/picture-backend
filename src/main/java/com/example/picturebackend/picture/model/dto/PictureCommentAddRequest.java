package com.example.picturebackend.picture.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureCommentAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String content;

    /** 被回复的评论 ID；为空表示发表根评论 */
    private Long parentId;
}
