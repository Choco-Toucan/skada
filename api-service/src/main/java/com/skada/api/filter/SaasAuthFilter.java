package com.skada.api.filter;

import com.google.gson.Gson;
import com.skada.api.mapper.TenantMapper;
import com.skada.api.model.Tenant;
import com.skada.common.enums.BizCode;
import com.skada.common.model.BaseResponse;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SAAS Open API 请求鉴权过滤器
 * <p>
 * 校验 header 中的 X-Tenant-Id / X-Sign / X-Timestamp，仅对 api-service 生效。
 * 如果没有携带 X-Tenant-Id，则跳过校验（允许匿名访问场景）。
 * </p>
 */
@Component
@Order(1)
public class SaasAuthFilter implements Filter {

    private static final long TIMESTAMP_TOLERANCE_MS = 5 * 60 * 1000L;
    private static final String SIGN_SALT = "skada";
    private static final String ATTR_TENANT_ID = "saasTenantId";

    private final TenantMapper tenantMapper;
    private final Gson gson;

    public SaasAuthFilter(TenantMapper tenantMapper, Gson gson) {
        this.tenantMapper = tenantMapper;
        this.gson = gson;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        String path = httpReq.getRequestURI();
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String tenantId = httpReq.getHeader("X-Tenant-Id");
        // 未携带租户ID，匿名请求，直接放行
        if (tenantId == null || tenantId.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        String sign = httpReq.getHeader("X-Sign");
        String timestampStr = httpReq.getHeader("X-Timestamp");

        // 校验参数完整性
        if (sign == null || sign.isBlank()) {
            writeError(httpResp, BizCode.SIGN_MISSING, "缺少 X-Sign 签名");
            return;
        }
        if (timestampStr == null || timestampStr.isBlank()) {
            writeError(httpResp, BizCode.TIMESTAMP_MISSING, "缺少 X-Timestamp 时间戳");
            return;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            writeError(httpResp, BizCode.TIMESTAMP_EXPIRED, "X-Timestamp 格式无效");
            return;
        }

        // 校验时间戳偏差
        long now = System.currentTimeMillis();
        if (Math.abs(now - timestamp) > TIMESTAMP_TOLERANCE_MS) {
            writeError(httpResp, BizCode.TIMESTAMP_EXPIRED, "请求时间戳已过期");
            return;
        }

        // 查询租户
        Tenant tenant = tenantMapper.findByTenantId(tenantId);
        if (tenant == null || !tenant.isEnabled()) {
            writeError(httpResp, BizCode.TENANT_NOT_FOUND_OR_DISABLED, "租户不存在或已停用");
            return;
        }

        // 签算校验：SHA256(timestamp + secretKey + skada)
        String expectedSign = sha256(timestamp + tenant.getSecretKey() + SIGN_SALT);
        if (!expectedSign.equals(sign)) {
            writeError(httpResp, BizCode.SIGN_INVALID, "签名校验失败");
            return;
        }

        // 鉴权通过，注入租户ID供下游使用
        httpReq.setAttribute(ATTR_TENANT_ID, tenantId);
        chain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(200);
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(gson.toJson(BaseResponse.error(code, message)));
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
