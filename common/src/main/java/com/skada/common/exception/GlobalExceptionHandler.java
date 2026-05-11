package com.skada.common.exception;

import com.skada.common.model.BaseResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    /** 请求体JSON格式错误 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        return BaseResponse.error(400, "请求体格式错误，请使用合法的JSON");
    }

    /** JSR-303 参数校验异常 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        return BaseResponse.error(400, message);
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<Void> handleException(Exception e) {
        log.error("未捕获异常", e);
        return BaseResponse.error(500, "服务器内部错误");
    }
}
