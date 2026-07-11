package com.example.picturebackend.picture.model.dto;

import com.example.picturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PictureCommentQueryRequest extends PageRequest {

    private static final long serialVersionUID = 1L;
}
