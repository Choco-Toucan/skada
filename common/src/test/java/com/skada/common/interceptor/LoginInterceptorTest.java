package com.skada.common.interceptor;

import com.google.gson.Gson;
import com.skada.common.annotation.SkipLoginCheck;
import com.skada.common.model.BaseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginInterceptor 单元测试")
class LoginInterceptorTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private Gson gson = new Gson();
    private LoginInterceptor interceptor;

    // 用于构造 HandlerMethod 的标记方法，定义在顶级类上方便反射查找
    @SkipLoginCheck
    void annotatedMethod() {}

    void plainMethod() {}

    @BeforeEach
    void setUp() {
        interceptor = new LoginInterceptor(redisTemplate, gson);
    }

    @Test
    @DisplayName("非 HandlerMethod 直接放行（静态资源等）")
    void nonHandlerMethod() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, "not a handler");

        assertThat(result).isTrue();
    }

    @Nested
    @DisplayName("@SkipLoginCheck 注解跳过")
    class SkipLoginCheckTests {

        @Test
        @DisplayName("方法上有 @SkipLoginCheck 则跳过校验")
        void methodAnnotation() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            HandlerMethod hm = handlerMethod("annotatedMethod");

            boolean result = interceptor.preHandle(request, response, hm);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Token 校验")
    class TokenValidation {

        @Test
        @DisplayName("无 Authorization header 返回 401")
        void noToken() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            HandlerMethod hm = handlerMethod("plainMethod");

            boolean result = interceptor.preHandle(request, response, hm);

            assertThat(result).isFalse();
            assertThat(response.getContentAsString()).contains("token为空");
        }

        @Test
        @DisplayName("空 Authorization header 返回 401")
        void emptyToken() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "");
            MockHttpServletResponse response = new MockHttpServletResponse();
            HandlerMethod hm = handlerMethod("plainMethod");

            boolean result = interceptor.preHandle(request, response, hm);

            assertThat(result).isFalse();
            assertThat(response.getContentAsString()).contains("token为空");
        }

        @Test
        @DisplayName("token 在 Redis 中不存在（已过期或无效）"
                + "")
        void tokenExpired() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer mytoken123");
            MockHttpServletResponse response = new MockHttpServletResponse();
            HandlerMethod hm = handlerMethod("plainMethod");

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("skada:token:mytoken123")).thenReturn(null);

            boolean result = interceptor.preHandle(request, response, hm);

            assertThat(result).isFalse();
            assertThat(response.getContentAsString()).contains("已过期或无效");
        }

        @Test
        @DisplayName("无 Bearer 前缀的 token 也可校验")
        void tokenWithoutBearer() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "mytoken123");
            MockHttpServletResponse response = new MockHttpServletResponse();
            HandlerMethod hm = handlerMethod("plainMethod");

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("skada:token:mytoken123")).thenReturn("ad_00000001:admin");
            when(redisTemplate.expire(anyString(), eq(7200L), eq(TimeUnit.SECONDS))).thenReturn(true);

            boolean result = interceptor.preHandle(request, response, hm);

            assertThat(result).isTrue();
            assertThat(request.getAttribute("adminId")).isEqualTo("ad_00000001");
            assertThat(request.getAttribute("adminRole")).isEqualTo("admin");
        }

        @Test
        @DisplayName("token 值仅含 adminId 不含分隔符时，只设置 adminId")
        void tokenValueWithoutRole() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer token1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            HandlerMethod hm = handlerMethod("plainMethod");

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("skada:token:token1")).thenReturn("ad_00000001");
            when(redisTemplate.expire(anyString(), eq(7200L), eq(TimeUnit.SECONDS))).thenReturn(true);

            boolean result = interceptor.preHandle(request, response, hm);

            assertThat(result).isTrue();
            assertThat(request.getAttribute("adminId")).isEqualTo("ad_00000001");
            assertThat(request.getAttribute("adminRole")).isNull();
        }
    }

    private HandlerMethod handlerMethod(String methodName) {
        try {
            Method m = LoginInterceptorTest.class.getDeclaredMethod(methodName);
            return new HandlerMethod(this, m);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
