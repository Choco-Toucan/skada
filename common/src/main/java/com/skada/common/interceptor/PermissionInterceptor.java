package com.skada.common.interceptor;

import com.skada.common.annotation.RequirePermission;
import com.skada.common.model.BaseResponse;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 权限校验拦截器
 * 根据 @RequirePermission 注解校验当前用户权限
 */
public class PermissionInterceptor implements HandlerInterceptor {

    private static final String ADMIN_PERMISSION_PREFIX = "skada:admin:permission:";

    private final StringRedisTemplate redisTemplate;
    private final Gson gson;

    public PermissionInterceptor(StringRedisTemplate redisTemplate, Gson gson) {
        this.redisTemplate = redisTemplate;
        this.gson = gson;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }

        RequirePermission annotation = hm.getMethodAnnotation(RequirePermission.class);
        if (annotation == null) {
            return true;
        }

        String adminId = (String) request.getAttribute("adminId");
        if (adminId == null) {
            writeError(response, 401, "未登录");
            return false;
        }

        // 从Redis获取管理员权限列表，校验是否包含所需权限
        String permissionValue = redisTemplate.opsForValue()
                .get(ADMIN_PERMISSION_PREFIX + adminId);
        if (permissionValue == null || !permissionValue.contains(annotation.value())) {
            writeError(response, 403, "权限不足");
            return false;
        }

        return true;
    }

    private void writeError(HttpServletResponse response, int code, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(200);
        response.getWriter().write(gson.toJson(BaseResponse.error(code, message)));
    }
}
