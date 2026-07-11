package com.example.picturebackend.picture.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String description;
}
