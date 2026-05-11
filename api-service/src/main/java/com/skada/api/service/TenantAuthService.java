package com.skada.api.service;

import com.skada.api.mapper.TenantMapper;
import com.skada.api.model.Tenant;
import org.springframework.stereotype.Service;

/**
 * 租户鉴权服务
 * 校验上报数据的租户身份
 */
@Service
public class TenantAuthService {

    private final TenantMapper tenantMapper;

    public TenantAuthService(TenantMapper tenantMapper) {
        this.tenantMapper = tenantMapper;
    }

    /**
     * 校验租户ID和密钥是否匹配
     * @return 校验成功返回租户信息，失败返回null
     */
    public Tenant authenticate(String tenantId, String secretKey) {
        if (tenantId == null || secretKey == null) {
            return null;
        }
        Tenant tenant = tenantMapper.findByTenantId(tenantId);
        if (tenant == null || tenant.getStatus() != 1) {
            return null;
        }
        if (!secretKey.equals(tenant.getSecretKey())) {
            return null;
        }
        return tenant;
    }
}
