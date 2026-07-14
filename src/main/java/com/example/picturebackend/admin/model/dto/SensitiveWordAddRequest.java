package com.example.picturebackend.admin.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SensitiveWordAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String word;
}
