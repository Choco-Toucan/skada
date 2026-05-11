package com.skada.common.interceptor;

import com.skada.common.annotation.SkipLoginCheck;
import com.skada.common.model.BaseResponse;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * 登录态校验拦截器
 * 除了带 @SkipLoginCheck 注解的方法，其他请求需要校验token
 * token校验通过后自动续期
 */
public class LoginInterceptor implements HandlerInterceptor {

    private static final String TOKEN_PREFIX = "skada:token:";
    private static final long TOKEN_EXPIRE_SECONDS = 7200; // 2小时

    private final StringRedisTemplate redisTemplate;
    private final Gson gson;

    public LoginInterceptor(StringRedisTemplate redisTemplate, Gson gson) {
        this.redisTemplate = redisTemplate;
        this.gson = gson;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 非HandlerMethod直接放行（如静态资源）
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }

        // 有 @SkipLoginCheck 注解则跳过
        if (hm.getMethodAnnotation(SkipLoginCheck.class) != null
                || hm.getBeanType().getAnnotation(SkipLoginCheck.class) != null) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            writeError(response, 401, "未登录或token为空");
            return false;
        }

        // 去掉 "Bearer " 前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        String adminId = redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
        if (adminId == null) {
            writeError(response, 401, "token已过期或无效");
            return false;
        }

        // 自动续期
        redisTemplate.expire(TOKEN_PREFIX + token, TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);

        request.setAttribute("adminId", adminId);
        return true;
    }

    private void writeError(HttpServletResponse response, int code, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(200);
        response.getWriter().write(gson.toJson(BaseResponse.error(code, message)));
    }
}
