package com.springboot.exception;

import com.springboot.common.BaseResponse;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
*
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<?>> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        BaseResponse<?> response = ResultUtils.error(e.getCode(), e.getMessage());
        return ResponseEntity.status(resolveHttpStatus(e.getCode())).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<BaseResponse<?>> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        BaseResponse<?> response = ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private HttpStatus resolveHttpStatus(int bizCode) {
        if (bizCode == ErrorCode.NOT_LOGIN_ERROR.getCode()) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (bizCode == ErrorCode.NO_AUTH_ERROR.getCode() || bizCode == ErrorCode.FORBIDDEN_ERROR.getCode()) {
            return HttpStatus.FORBIDDEN;
        }
        if (bizCode >= 40000 && bizCode < 50000) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
