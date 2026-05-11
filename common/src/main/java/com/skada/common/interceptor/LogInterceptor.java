package com.skada.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

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

        // 请求参数：GET取query string，POST暂记录content type
        String params;
        if ("GET".equalsIgnoreCase(method)) {
            params = request.getQueryString();
        } else {
            params = "[body]";
        }

        API_LOG.info("{} {} -> {} ({}ms) params={}", method, uri, status, cost, params);

        START_TIME.remove();
    }
}
