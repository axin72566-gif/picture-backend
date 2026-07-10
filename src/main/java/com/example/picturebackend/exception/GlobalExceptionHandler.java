package com.example.picturebackend.exception;

import com.example.picturebackend.common.BaseResponse;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<Void> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常", e);
        return ResultUtils.error(ErrorCode.SERVER_ERROR, e.getMessage());
    }
}