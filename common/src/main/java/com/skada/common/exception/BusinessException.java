package com.skada.common.exception;

/**
 * 业务异常
 * 用于在Service层抛出明确的业务错误
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(400, message);
    }

    public int getCode() {
        return code;
    }
}
