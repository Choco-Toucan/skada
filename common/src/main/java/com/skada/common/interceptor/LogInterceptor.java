package com.skada.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * 接口日志拦截器
 * 记录请求参数、响应、调用耗时
 */
public class LogInterceptor implements HandlerInterceptor {

    private static final Logger API_LOG = LogManager.getLogger("ApiLog");

    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        START_TIME.set(System.currentTimeMillis());
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
        // 不在此处处理，在 afterCompletion 中统一处理
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Long start = START_TIME.get();
        if (start == null) {
            return;
        }
        long cost = System.currentTimeMillis() - start;
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();

        // 请求参数
        String params;
        if ("GET".equalsIgnoreCase(method)) {
            params = request.getQueryString();
        } else {
            params = getRequestBody(request);
        }

        // 响应体
        String responseBody = getResponseBody(response);

        API_LOG.info("{} {} -> {} ({}ms) params={} response={}", method, uri, status, cost, params, responseBody);

        START_TIME.remove();
    }

    private String getRequestBody(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            byte[] content = wrapper.getContentAsByteArray();
            if (content.length > 0) {
                String body = new String(content, java.nio.charset.StandardCharsets.UTF_8);
                // 截断过长body，最大4096字符
                if (body.length() > 4096) {
                    body = body.substring(0, 4096) + "...[truncated]";
                }
                return body;
            }
        }
        return "[no body]";
    }

    private String getResponseBody(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper wrapper) {
            byte[] content = wrapper.getContentAsByteArray();
            if (content.length > 0) {
                String body = new String(content, java.nio.charset.StandardCharsets.UTF_8);
                if (body.length() > 4096) {
                    body = body.substring(0, 4096) + "...[truncated]";
                }
                return body;
            }
        }
        return "[no body]";
    }
}
