package com.skada.common.interceptor;

import com.skada.common.annotation.RequirePermission;
import com.skada.common.model.BaseResponse;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 权限校验拦截器
 * 根据 @RequirePermission 注解校验当前用户角色是否满足权限要求
 */
public class PermissionInterceptor implements HandlerInterceptor {

    private final Gson gson;

    public PermissionInterceptor(Gson gson) {
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

        String role = (String) request.getAttribute("adminRole");
        if (role == null) {
            writeError(response, 401, "未登录");
            return false;
        }

        // 当前用户的角色等于注解要求的权限标识则放行
        if (!role.equals(annotation.value())) {
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
