package com.skada.common.exception;

import com.skada.common.enums.BizCode;
import com.skada.common.model.BaseResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 捕获所有未处理异常，返回统一的错误响应（HTTP状态始终为200）
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LogManager.getLogger(GlobalExceptionHandler.class);

    /** 参数校验失败（控制器层手动校验） */
    @ExceptionHandler(IllegalArgumentException.class)
    public BaseResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        return BaseResponse.error(BizCode.PARAM_MISSING, e.getMessage());
    }

    /** 业务异常，使用异常自身携带的code */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<Void> handleBusiness(BusinessException e) {
        return BaseResponse.error(e.getCode(), e.getMessage());
    }

    /** 请求体JSON格式错误 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        return BaseResponse.error(BizCode.REQUEST_FORMAT_ERROR, "请求体格式错误，请使用合法的JSON");
    }

    /** JSR-303 参数校验异常 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        return BaseResponse.error(BizCode.PARAM_MISSING, message);
    }

    /** 未捕获的其他异常，统一作为内部错误返回，不暴露异常细节 */
    @ExceptionHandler(Exception.class)
    public BaseResponse<Void> handleException(Exception e) {
        log.error("未捕获异常", e);
        return BaseResponse.error(BizCode.INTERNAL_ERROR, "服务器内部错误");
    }
}
