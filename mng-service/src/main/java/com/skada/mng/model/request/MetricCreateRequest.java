package com.skada.mng.model.request;

/**
 * 创建指标请求
 */
public class MetricCreateRequest {

    private String tenantId;
    private String name;
    private String description;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
