package com.guat.cloud.user.exception;

import com.guat.cloud.user.dto.Result;
import io.jsonwebtoken.JwtException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 全局异常处理器
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    // 处理 JWT 相关异常
    @ExceptionHandler(JwtException.class)
    @ResponseBody
    public Result handleJwtException(JwtException e) {

        return Result.error(e.getMessage());
    }

    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public Result handleBusinessException(BusinessException e) {
        return Result.error(e.getMessage());
    }

    // 处理其他通用异常
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result handleGeneralException(Exception e) {
        // 记录详细错误日志
        e.printStackTrace();
        return Result.error("服务器内部错误");
    }
}