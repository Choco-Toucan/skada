package com.skada.api.model.request;

/**
 * 通过API触发排行榜滚动请求
 * <p>租户身份由 SaasAuthFilter 从 header 校验后注入，body 仅包含业务参数。</p>
 */
public class RollLeaderboardRequest {

    private String planId;
    private String instanceId;

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
}
