package com.skada.common.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentCachingFilter 单元测试")
class ContentCachingFilterTest {

    @Mock
    private FilterChain chain;

    private ContentCachingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ContentCachingFilter();
    }

    @Test
    @DisplayName("将 request 和 response 包装为 ContentCaching 版本")
    void wrapsRequestAndResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
        request.setContent("{\"key\":\"value\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        ArgumentCaptor<jakarta.servlet.ServletRequest> reqCaptor = ArgumentCaptor.forClass(jakarta.servlet.ServletRequest.class);
        ArgumentCaptor<jakarta.servlet.ServletResponse> respCaptor = ArgumentCaptor.forClass(jakarta.servlet.ServletResponse.class);
        verify(chain).doFilter(reqCaptor.capture(), respCaptor.capture());

        assertThat(reqCaptor.getValue()).isInstanceOf(ContentCachingRequestWrapper.class);
        assertThat(respCaptor.getValue()).isInstanceOf(ContentCachingResponseWrapper.class);
    }

    @Test
    @DisplayName("包装后的 request "
            + "仍需能读取原始body")
    void wrappedRequestPreservesBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
        request.setContent("{\"key\":\"value\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, resp) -> {
            ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) req;
            byte[] content = wrapper.getInputStream().readAllBytes();
            assertThat(new String(content)).isEqualTo("{\"key\":\"value\"}");
        });
    }
}
