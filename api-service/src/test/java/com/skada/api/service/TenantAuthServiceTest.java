package com.skada.api.service;

import com.skada.api.mapper.TenantMapper;
import com.skada.api.model.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantAuthService 单元测试")
class TenantAuthServiceTest {

    @Mock
    private TenantMapper tenantMapper;

    private TenantAuthService authService;

    private static final String TENANT_ID = "tenant-001";
    private static final String SECRET_KEY = "secret-123";

    @BeforeEach
    void setUp() {
        authService = new TenantAuthService(tenantMapper);
    }

    @Test
    @DisplayName("authenticate: 验证成功返回租户信息")
    void authenticate_success() {
        Tenant tenant = new Tenant();
        tenant.setTenantId(TENANT_ID);
        tenant.setSecretKey(SECRET_KEY);
        tenant.setStatus(1);
        when(tenantMapper.findByTenantId(TENANT_ID)).thenReturn(tenant);

        Tenant result = authService.authenticate(TENANT_ID, SECRET_KEY);

        assertThat(result).isNotNull();
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    @DisplayName("authenticate: tenantId为null时返回null")
    void authenticate_nullTenantId() {
        Tenant result = authService.authenticate(null, SECRET_KEY);
        assertThat(result).isNull();
        verifyNoInteractions(tenantMapper);
    }

    @Test
    @DisplayName("authenticate: secretKey为null时返回null")
    void authenticate_nullSecretKey() {
        Tenant result = authService.authenticate(TENANT_ID, null);
        assertThat(result).isNull();
        verifyNoInteractions(tenantMapper);
    }

    @Test
    @DisplayName("authenticate: 租户不存在时返回null")
    void authenticate_tenantNotFound() {
        when(tenantMapper.findByTenantId(TENANT_ID)).thenReturn(null);

        Tenant result = authService.authenticate(TENANT_ID, SECRET_KEY);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("authenticate: 租户已停用时返回null")
    void authenticate_tenantDisabled() {
        Tenant tenant = new Tenant();
        tenant.setTenantId(TENANT_ID);
        tenant.setSecretKey(SECRET_KEY);
        tenant.setStatus(0);
        when(tenantMapper.findByTenantId(TENANT_ID)).thenReturn(tenant);

        Tenant result = authService.authenticate(TENANT_ID, SECRET_KEY);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("authenticate: 密钥不匹配时返回null")
    void authenticate_wrongSecret() {
        Tenant tenant = new Tenant();
        tenant.setTenantId(TENANT_ID);
        tenant.setSecretKey("correct-secret");
        tenant.setStatus(1);
        when(tenantMapper.findByTenantId(TENANT_ID)).thenReturn(tenant);

        Tenant result = authService.authenticate(TENANT_ID, "wrong-secret");

        assertThat(result).isNull();
    }
}
