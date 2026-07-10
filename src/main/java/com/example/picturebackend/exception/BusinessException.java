package com.example.picturebackend.exception;

import com.example.picturebackend.common.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    private final String description;

    public BusinessException(String message) {
        super(message);
        this.code = ErrorCode.SERVER_ERROR.getCode();
        this.description = message;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = errorCode.getMessage();
    }

    public BusinessException(ErrorCode errorCode, String description) {
        super(description);
        this.code = errorCode.getCode();
        this.description = description;
    }

    public BusinessException(int code, String description) {
        super(description);
        this.code = code;
        this.description = description;
    }
}