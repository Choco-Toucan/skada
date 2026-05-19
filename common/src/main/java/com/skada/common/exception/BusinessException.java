package com.skada.common.exception;

import com.skada.common.enums.BizCode;

/**
 * 业务异常
 * 用于在Service层抛出明确的业务错误，code使用 {@link BizCode} 中的常量
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 使用默认通用业务错误码（30000）
     */
    public BusinessException(String message) {
        this(BizCode.BIZ_ERROR, message);
    }

    public int getCode() {
        return code;
    }
}
