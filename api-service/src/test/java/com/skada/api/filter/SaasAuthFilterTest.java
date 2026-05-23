package com.skada.api.filter;

import com.google.gson.Gson;
import com.skada.api.mapper.TenantMapper;
import com.skada.api.model.Tenant;
import com.skada.common.enums.BizCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SaasAuthFilter 单元测试")
class SaasAuthFilterTest {

    @Mock private TenantMapper tenantMapper;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;

    private SaasAuthFilter filter;
    private Gson gson = new Gson();

    private static final String TENANT_ID = "tenant-001";
    private static final String SECRET_KEY = "test-secret";

    @BeforeEach
    void setUp() {
        filter = new SaasAuthFilter(tenantMapper, gson);
    }

    /** 计算合法签名 */
    private String validSign(long timestamp) {
        return sha256(timestamp + SECRET_KEY + "skada");
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Nested
    @DisplayName("路径过滤")
    class PathFiltering {

        @Test
        @DisplayName("非/api/路径直接放行")
        void nonApiPath_passesThrough() throws Exception {
            when(request.getRequestURI()).thenReturn("/health");

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            verifyNoInteractions(tenantMapper);
        }

        @Test
        @DisplayName("/api/路径进入鉴权逻辑")
        void apiPath_entersAuth() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/v1/score/submit");
            when(request.getHeader("X-Tenant-Id")).thenReturn(null);

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("匿名访问")
    class AnonymousAccess {

        @Test
        @DisplayName("无X-Tenant-Id时匿名放行")
        void noTenantId_anonymousPass() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/v1/leaderboard/ranking");
            when(request.getHeader("X-Tenant-Id")).thenReturn(null);

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("空X-Tenant-Id时匿名放行")
        void blankTenantId_anonymousPass() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/v1/leaderboard/ranking");
            when(request.getHeader("X-Tenant-Id")).thenReturn("");

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("签名校验")
    class SignatureValidation {

        @Test
        @DisplayName("缺少X-Sign时返回错误")
        void missingSign_returnsError() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/v1/score/submit");
            when(request.getHeader("X-Tenant-Id")).thenReturn(TENANT_ID);
            when(request.getHeader("X-Sign")).thenReturn(null);

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            when(response.getWriter()).thenReturn(pw);

            filter.doFilter(request, response, chain);

            verify(response).setStatus(200);
            verify(response).setContentType("application/json; charset=UTF-8");
            verify(chain, never()).doFilter(any(), any());
            assertThat(sw.toString()).contains("缺少 X-Sign");
        }

        @Test
        @DisplayName("缺少X-Timestamp时返回错误")
        void missingTimestamp_returnsError() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/v1/score/submit");
            when(request.getHeader("X-Tenant-Id")).thenReturn(TENANT_ID);
            when(request.getHeader("X-Sign")).thenReturn("some-sign");
            when(request.getHeader("X-Timestamp")).thenReturn(null);

            StringWriter sw = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(sw));

            filter.doFilter(request, response, chain);

            verify(chain, never()).doFilter(any(), any());
            assertThat(sw.toString()).contains("缺少 X-Timestamp");
        }

        @Test
        @DisplayName("X-Timestamp格式无效时返回错误")
        void invalidTimestamp_returnsError() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/v1/score/submit");
            when(request.getHeader("X-Tenant-Id")).thenReturn(TENANT_ID);
            when(request.getHeader("X-Sign")).thenReturn("some-sign");
            when(request.getHeader("X-Timestamp")).thenReturn("not-a-number");

            StringWriter sw = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(sw));

            filter.doFilter(request, response, chain);

            verify(chain, never()).doFilter(any(), any());
            assertThat(sw.toString()).contains("格式无效");
        }

        @Test
        @DisplayName("时间戳过期时返回错误")
        void expiredTimestamp_returnsError() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/v1/score/submit");
            when(request.getHeader("X-Tenant-Id")).thenReturn(TENANT_ID);
            when(request.getHeader("X-Sign")).thenReturn("some-sign");

            long expired = System.currentTimeMillis() - 10 * 60 * 1000;
            when(request.getHeader("X-Timestamp")).thenReturn(String.valueOf(expired));

            StringWriter sw = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(sw));

            filter.doFilter(request, response, chain);

            verify(chain, never()).doFilter(any(), any());
            assertThat(sw.toString()).contains("已过期");
        }

        @Test
        @DisplayName("租户不存在或已停用时返回错误")
        void tenantNotFound_returnsError() throws Exception {
            long now = System.currentTimeMillis();
            when(request.getRequestURI()).thenReturn("/api/v1/score/submit");
            when(request.getHeader("X-Tenant-Id")).thenReturn(TENANT_ID);
            when(request.getHeader("X-Sign")).thenReturn("some-sign");
            when(request.getHeader("X-Timestamp")).thenReturn(String.valueOf(now));
            when(tenantMapper.findByTenantId(TENANT_ID)).thenReturn(null);

            StringWriter sw = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(sw));

            filter.doFilter(request, response, chain);

            verify(chain, never()).doFilter(any(), any());
            assertThat(sw.toString()).contains("租户不存在或已停用");
        }

        @Test
        @DisplayName("签名不匹配时返回错误")
        void signMismatch_returnsError() throws Exception {
            long now = System.currentTimeMillis();
            when(request.getRequestURI()).thenReturn("/api/v1/score/submit");
            when(request.getHeader("X-Tenant-Id")).thenReturn(TENANT_ID);
            when(request.getHeader("X-Sign")).thenReturn("wrong-sign");
            when(request.getHeader("X-Timestamp")).thenReturn(String.valueOf(now));

            Tenant tenant = new Tenant();
            tenant.setTenantId(TENANT_ID);
            tenant.setSecretKey(SECRET_KEY);
            tenant.setStatus(1);
            when(tenantMapper.findByTenantId(TENANT_ID)).thenReturn(tenant);

            StringWriter sw = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(sw));

            filter.doFilter(request, response, chain);

            verify(chain, never()).doFilter(any(), any());
            assertThat(sw.toString()).contains("签名校验失败");
        }
    }

    @Nested
    @DisplayName("鉴权成功")
    class AuthSuccess {

        @Test
        @DisplayName("签名校验通过后放行并注入tenantId")
        void validSign_passesAndInjectsTenantId() throws Exception {
            long now = System.currentTimeMillis();
            String sign = validSign(now);

            when(request.getRequestURI()).thenReturn("/api/v1/score/submit");
            when(request.getHeader("X-Tenant-Id")).thenReturn(TENANT_ID);
            when(request.getHeader("X-Sign")).thenReturn(sign);
            when(request.getHeader("X-Timestamp")).thenReturn(String.valueOf(now));

            Tenant tenant = new Tenant();
            tenant.setTenantId(TENANT_ID);
            tenant.setSecretKey(SECRET_KEY);
            tenant.setStatus(1);
            when(tenantMapper.findByTenantId(TENANT_ID)).thenReturn(tenant);

            filter.doFilter(request, response, chain);

            verify(request).setAttribute("saasTenantId", TENANT_ID);
            verify(chain).doFilter(request, response);
        }
    }
}
