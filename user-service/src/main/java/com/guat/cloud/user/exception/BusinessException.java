package com.guat.cloud.user.exception;

import com.guat.cloud.user.dto.Result;

/**
 * 业务异常：Service 层抛出，由 GlobalExceptionHandler 统一转成 Result
 */
public class BusinessException extends RuntimeException {

    private int code;

    public BusinessException(String message) {
        this(Result.CODE_BAD_REQUEST, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}