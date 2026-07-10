package com.example.picturebackend.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN(40100, "未登录"),
    NO_AUTH(40101, "无权限"),
    TOO_MANY_REQUEST(42900, "请求过于频繁"),
    SERVER_ERROR(50000, "服务器内部错误"),

    USER_EXIST(41001, "账号已存在"),
    USER_NOT_FOUND(41002, "用户不存在"),
    PASSWORD_ERROR(41003, "密码错误"),
    ACCOUNT_FORBIDDEN(41004, "账号已被禁用");

    private final int code;

    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}