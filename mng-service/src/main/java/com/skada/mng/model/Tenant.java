package com.skada.mng.model;

import com.skada.common.enums.TenantStatus;

/**
 * 租户实体
 */
public class Tenant {

    private Long id;
    private String tenantId;
    private String name;
    private String secretKey;
    private Integer allowAnonymousQuery;
    private Integer status;
    private String createTime;
    private String updateTime;
    private String createBy;
    private String updateBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public Integer getAllowAnonymousQuery() { return allowAnonymousQuery; }
    public void setAllowAnonymousQuery(Integer allowAnonymousQuery) { this.allowAnonymousQuery = allowAnonymousQuery; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    /** 获取状态枚举 */
    public TenantStatus getStatusEnum() { return TenantStatus.fromValue(status); }
    /** 通过枚举设置状态 */
    public void setStatusEnum(TenantStatus s) { this.status = s.getValue(); }
    /** 租户是否启用 */
    public boolean isEnabled() { return getStatusEnum().isEnabled(); }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
}
