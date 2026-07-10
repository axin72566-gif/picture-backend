package com.example.picturebackend.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private int current = 1;

    private int pageSize = 10;

    private String sortField;

    private String sortOrder = "desc";
}