package com.skada.common.interceptor;

import com.skada.common.model.BaseResponse;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 数据校验拦截器
 * <p>
 * 对进入Controller的请求做前置校验：
 * 1. POST/PUT请求必须声明 Content-Type: application/json
 * 2. 拒绝明显超大的请求体（防止内存炸弹）
 * 3. 基本参数格式校验
 * </p>
 */
public class ValidationInterceptor implements HandlerInterceptor {

    /** 请求体最大长度 1MB */
    private static final int MAX_BODY_LENGTH = 1_048_576;

    private final Gson gson;

    public ValidationInterceptor(Gson gson) {
        this.gson = gson;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String method = request.getMethod().toUpperCase();

        // POST/PUT 请求必须带 Content-Type: application/json
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            String contentType = request.getContentType();
            if (contentType == null || !contentType.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE)) {
                writeError(response, 400, "请求Content-Type必须为application/json");
                return false;
            }

            int contentLength = request.getContentLength();
            if (contentLength > MAX_BODY_LENGTH) {
                writeError(response, 400, "请求体过大，最大允许1MB");
                return false;
            }
        }

        return true;
    }

    private void writeError(HttpServletResponse response, int code, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(200);
        response.getWriter().write(gson.toJson(BaseResponse.error(code, message)));
    }
}
