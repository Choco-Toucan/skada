package com.skada.api.model.response;

/**
 * 排行榜滚动响应
 */
public class RollLeaderboardResponse {

    private String planId;
    private String instanceId;
    private Integer instanceSeq;

    public RollLeaderboardResponse(String planId, String instanceId, Integer instanceSeq) {
        this.planId = planId;
        this.instanceId = instanceId;
        this.instanceSeq = instanceSeq;
    }

    public String getPlanId() { return planId; }
    public String getInstanceId() { return instanceId; }
    public Integer getInstanceSeq() { return instanceSeq; }
}
