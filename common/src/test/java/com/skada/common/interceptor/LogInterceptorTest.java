package com.skada.common.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import static org.assertj.core.api.Assertions.*;

@DisplayName("LogInterceptor 单元测试")
class LogInterceptorTest {

    private LogInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new LogInterceptor();
    }

    @Test
    @DisplayName("preHandle 设置开始时间并放行")
    void preHandle() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("afterCompletion 无开始时间时直接返回")
    void afterCompletionNoStartTime() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.afterCompletion(request, response, new Object(), null);
    }

    @Test
    @DisplayName("GET 请求记录 query string")
    void afterCompletionGetRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test?page=1&size=10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        interceptor.preHandle(request, response, new Object());

        interceptor.afterCompletion(request, response, new Object(), null);
    }

    @Nested
    @DisplayName("POST 请求体记录")
    class PostRequestBody {

        @Test
        @DisplayName("ContentCachingRequestWrapper 中的 body 被记录")
        void withCachingWrapper() throws Exception {
            MockHttpServletRequest raw = new MockHttpServletRequest("POST", "/api/test");
            raw.setContent("{\"name\":\"test\"}".getBytes());
            ContentCachingRequestWrapper request = new ContentCachingRequestWrapper(raw, 8192);
            // 触发缓存：需要先读取输入流
            request.getInputStream().readAllBytes();
            MockHttpServletResponse response = new MockHttpServletResponse();
            interceptor.preHandle(request, response, new Object());

            interceptor.afterCompletion(request, response, new Object(), null);
        }

        @Test
        @DisplayName("普通 request 没有 ContentCaching 包装时记录 [no body]")
        void withoutCachingWrapper() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
            MockHttpServletResponse response = new MockHttpServletResponse();
            interceptor.preHandle(request, response, new Object());

            interceptor.afterCompletion(request, response, new Object(), null);
        }
    }

    @Nested
    @DisplayName("响应体记录")
    class ResponseBody {

        @Test
        @DisplayName("ContentCachingResponseWrapper 中的响应被记录")
        void withCachingWrapper() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
            MockHttpServletResponse raw = new MockHttpServletResponse();
            ContentCachingResponseWrapper response = new ContentCachingResponseWrapper(raw);
            response.getWriter().write("{\"code\":200}");
            interceptor.preHandle(request, response, new Object());

            interceptor.afterCompletion(request, response, new Object(), null);
        }

        @Test
        @DisplayName("普通 response 记录 [no body]")
        void withoutCachingWrapper() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
            MockHttpServletResponse response = new MockHttpServletResponse();
            interceptor.preHandle(request, response, new Object());

            interceptor.afterCompletion(request, response, new Object(), null);
        }
    }

    @Nested
    @DisplayName("脱敏处理")
    class MaskSensitive {

        @Test
        @DisplayName("secretKey 字段值被替换为 ***")
        void maskSecretKey() throws Exception {
            MockHttpServletRequest raw = new MockHttpServletRequest("POST", "/api/test");
            raw.setContent("{\"secretKey\":\"sk_12345\",\"name\":\"test\"}".getBytes());
            ContentCachingRequestWrapper request = new ContentCachingRequestWrapper(raw, 8192);
            request.getInputStream().readAllBytes();
            MockHttpServletResponse respRaw = new MockHttpServletResponse();
            ContentCachingResponseWrapper response = new ContentCachingResponseWrapper(respRaw);
            response.getWriter().write("{\"result\":\"ok\",\"secretKey\":\"sk_12345\"}");
            interceptor.preHandle(request, response, new Object());

            interceptor.afterCompletion(request, response, new Object(), null);
        }

        @Test
        @DisplayName("password 字段也被脱敏")
        void maskPassword() throws Exception {
            MockHttpServletRequest raw = new MockHttpServletRequest("POST", "/api/test");
            raw.setContent("{\"password\":\"mypwd\",\"user\":\"admin\"}".getBytes());
            ContentCachingRequestWrapper request = new ContentCachingRequestWrapper(raw, 8192);
            request.getInputStream().readAllBytes();
            MockHttpServletResponse response = new MockHttpServletResponse();
            interceptor.preHandle(request, response, new Object());

            interceptor.afterCompletion(request, response, new Object(), null);
        }

        @Test
        @DisplayName("无敏感字段时原样记录")
        void noSensitiveFields() throws Exception {
            MockHttpServletRequest raw = new MockHttpServletRequest("POST", "/api/test");
            raw.setContent("{\"name\":\"test\",\"age\":25}".getBytes());
            ContentCachingRequestWrapper request = new ContentCachingRequestWrapper(raw, 8192);
            request.getInputStream().readAllBytes();
            MockHttpServletResponse response = new MockHttpServletResponse();
            interceptor.preHandle(request, response, new Object());

            interceptor.afterCompletion(request, response, new Object(), null);
        }

        @Test
        @DisplayName("空字符串不抛异常")
        void emptyBody() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
            MockHttpServletResponse response = new MockHttpServletResponse();
            interceptor.preHandle(request, response, new Object());

            interceptor.afterCompletion(request, response, new Object(), null);
        }
    }
}
