package com.skada.common.interceptor;

import com.google.gson.Gson;
import com.skada.common.annotation.RequirePermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PermissionInterceptor 单元测试")
class PermissionInterceptorTest {

    private Gson gson = new Gson();
    private PermissionInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new PermissionInterceptor(gson);
    }

    @Test
    @DisplayName("非 HandlerMethod 直接放行")
    void nonHandlerMethod() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, "not a handler");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("无 @RequirePermission 注解的方法直接放行")
    void noAnnotation() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod hm = handlerMethod("plainMethod");

        boolean result = interceptor.preHandle(request, response, hm);

        assertThat(result).isTrue();
    }

    void plainMethod() {}

    @RequirePermission("admin")
    void adminMethod() {}

    @Nested
    @DisplayName("@RequirePermission 校验")
    class RequirePermissionTests {

        @Test
        @DisplayName("角色匹配则放行")
        void roleMatches() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setAttribute("adminRole", "admin");
            MockHttpServletResponse response = new MockHttpServletResponse();
            HandlerMethod hm = handlerMethod("adminMethod");

            boolean result = interceptor.preHandle(request, response, hm);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("角色不匹配返回 403")
        void roleMismatch() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setAttribute("adminRole", "viewer");
            MockHttpServletResponse response = new MockHttpServletResponse();
            HandlerMethod hm = handlerMethod("adminMethod");

            boolean result = interceptor.preHandle(request, response, hm);

            assertThat(result).isFalse();
            assertThat(response.getContentAsString()).contains("权限不足");
        }

        @Test
        @DisplayName("未登录（adminRole 为 null）返回 401")
        void notLoggedIn() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            HandlerMethod hm = handlerMethod("adminMethod");

            boolean result = interceptor.preHandle(request, response, hm);

            assertThat(result).isFalse();
            assertThat(response.getContentAsString()).contains("未登录");
        }
    }

    private HandlerMethod handlerMethod(String methodName) {
        try {
            Method m = PermissionInterceptorTest.class.getDeclaredMethod(methodName);
            return new HandlerMethod(this, m);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
