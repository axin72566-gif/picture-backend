package com.example.picturebackend.picture.model.dto;

import com.example.picturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PictureQueryRequest extends PageRequest {

    private String name;

    private Long minSize;

    private Long maxSize;
}
