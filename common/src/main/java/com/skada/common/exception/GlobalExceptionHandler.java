package com.skada.common.exception;

import com.skada.common.model.BaseResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 捕获所有未处理异常，返回统一的错误响应
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LogManager.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public BaseResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        return BaseResponse.error(400, e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<Void> handleBusiness(BusinessException e) {
        return BaseResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<Void> handleException(Exception e) {
        log.error("未捕获异常", e);
        return BaseResponse.error(500, "服务器内部错误");
    }
}
