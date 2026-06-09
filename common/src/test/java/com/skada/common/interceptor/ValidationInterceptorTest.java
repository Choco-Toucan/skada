package com.skada.common.interceptor;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ValidationInterceptor 单元测试")
class ValidationInterceptorTest {

    private Gson gson = new Gson();
    private ValidationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new ValidationInterceptor(gson);
    }

    @Test
    @DisplayName("GET 请求直接放行")
    void getRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Nested
    @DisplayName("POST/PUT/PATCH Content-Type 校验")
    class ContentTypeCheck {

        @Test
        @DisplayName("POST 无 Content-Type 返回 400")
        void postNoContentType() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean result = interceptor.preHandle(request, response, new Object());

            assertThat(result).isFalse();
            assertThat(response.getContentAsString()).contains("Content-Type必须为application/json");
        }

        @Test
        @DisplayName("POST Content-Type 非 application/json 返回 400")
        void postWrongContentType() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
            request.setContentType("text/plain");
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean result = interceptor.preHandle(request, response, new Object());

            assertThat(result).isFalse();
            assertThat(response.getContentAsString()).contains("Content-Type必须为application/json");
        }

        @Test
        @DisplayName("POST Content-Type 大小写不敏感（"
                + "APPLICATION/JSON）")
        void postContentTypeCaseInsensitive() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
            request.setContentType("APPLICATION/JSON");
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean result = interceptor.preHandle(request, response, new Object());

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("POST 正确 Content-Type 放行")
        void postCorrectContentType() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
            request.setContentType("application/json");
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean result = interceptor.preHandle(request, response, new Object());

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("请求体大小校验")
    class BodySizeCheck {

        @Test
        @DisplayName("请求体超过 1MB 返回 400")
        void bodyTooLarge() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test") {
                @Override
                public int getContentLength() { return 2_000_000; }
            };
            request.setContentType("application/json");
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean result = interceptor.preHandle(request, response, new Object());

            assertThat(result).isFalse();
            assertThat(response.getContentAsString()).contains("请求体过大");
        }

        @Test
        @DisplayName("请求体在 1MB 以内放行")
        void bodyWithinLimit() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test") {
                @Override
                public int getContentLength() { return 1_000_000; }
            };
            request.setContentType("application/json");
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean result = interceptor.preHandle(request, response, new Object());

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("无 contentLength 也放行")
        void noContentLength() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test") {
                @Override
                public int getContentLength() { return -1; }
            };
            request.setContentType("application/json");
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean result = interceptor.preHandle(request, response, new Object());

            assertThat(result).isTrue();
        }
    }
}
