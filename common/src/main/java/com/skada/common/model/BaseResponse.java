package com.skada.common.model;

import com.google.gson.annotations.SerializedName;
import com.skada.common.enums.BizCode;

/**
 * 统一API响应格式
 * <p>HTTP状态码始终返回200，业务状态通过 {@code code} 字段区分（参考 {@link BizCode}）。</p>
 */
public class BaseResponse<T> {

    @SerializedName("code")
    private int code;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private T data;

    @SerializedName("timestamp")
    private long timestamp;

    private BaseResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> BaseResponse<T> success(T data) {
        BaseResponse<T> resp = new BaseResponse<>();
        resp.code = 200;
        resp.message = "success";
        resp.data = data;
        return resp;
    }

    public static <T> BaseResponse<T> success() {
        return success(null);
    }

    public static <T> BaseResponse<T> error(int code, String message) {
        BaseResponse<T> resp = new BaseResponse<>();
        resp.code = code;
        resp.message = message;
        return resp;
    }

    public static <T> BaseResponse<T> error(String message) {
        return error(BizCode.SYSTEM_ERROR, message);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
