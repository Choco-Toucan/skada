package com.skada.mng.service;

import com.skada.common.exception.BusinessException;
import com.skada.mng.mapper.TenantMapper;
import com.skada.mng.model.Tenant;
import com.skada.mng.model.request.TenantCreateRequest;
import com.skada.mng.model.request.TenantUpdateRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 租户管理服务
 * 负责租户的创建、查询和更新
 */
@Service
public class TenantService {

    private final TenantMapper tenantMapper;

    public TenantService(TenantMapper tenantMapper) {
        this.tenantMapper = tenantMapper;
    }

    /**
     * 创建租户
     * 自动生成 tenant_id 和 secret_key
     */
    public Tenant create(TenantCreateRequest request, String adminId) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException("租户名称不能为空");
        }

        Tenant tenant = new Tenant();
        // 生成租户ID: tn_ + 8位随机字符
        tenant.setTenantId("tn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        // 生成密钥
        tenant.setSecretKey("sk_" + UUID.randomUUID().toString().replace("-", ""));
        tenant.setName(request.getName().trim());
        tenant.setAllowAnonymousQuery(request.getAllowAnonymousQuery() != null ? request.getAllowAnonymousQuery() : 0);
        tenant.setCreateBy(adminId);
        tenant.setUpdateBy(adminId);

        tenantMapper.insert(tenant);
        return tenant;
    }

    /**
     * 更新租户信息
     */
    public Tenant update(TenantUpdateRequest request, String adminId) {
        Tenant tenant = tenantMapper.findById(request.getId());
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            tenant.setName(request.getName().trim());
        }
        if (request.getAllowAnonymousQuery() != null) {
            tenant.setAllowAnonymousQuery(request.getAllowAnonymousQuery());
        }
        if (request.getStatus() != null) {
            tenant.setStatus(request.getStatus());
        }
        tenant.setUpdateBy(adminId);

        tenantMapper.update(tenant);
        return tenantMapper.findById(tenant.getId());
    }

    /**
     * 查询所有租户
     */
    public List<Tenant> findAll() {
        return tenantMapper.findAll();
    }

    /**
     * 根据ID查询租户
     */
    public Tenant findById(Long id) {
        Tenant tenant = tenantMapper.findById(id);
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }
        return tenant;
    }

    /**
     * 根据租户ID查询
     */
    public Tenant findByTenantId(String tenantId) {
        return tenantMapper.findByTenantId(tenantId);
    }
}
