package com.example.picturebackend.user.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class LoginUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;

    private UserVO user;
}