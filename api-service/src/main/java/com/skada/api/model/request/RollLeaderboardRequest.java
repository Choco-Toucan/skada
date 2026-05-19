package com.skada.api.model.request;

/**
 * 通过API触发排行榜滚动请求
 */
public class RollLeaderboardRequest {

    private String tenantId;
    private String secretKey;
    private String planId;
    private String instanceId;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
}
