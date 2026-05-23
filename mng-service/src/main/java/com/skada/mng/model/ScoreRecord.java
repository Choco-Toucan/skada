package com.skada.mng.model;

import java.math.BigDecimal;

/**
 * 分数记录实体（管理后台只读查询用）
 */
public class ScoreRecord {

    private Long id;
    private String tenantId;
    private Long leaderboardId;
    private Long instanceId;
    private Long metricId;
    private String userId;
    private BigDecimal score;
    private String payload;

    private String metricName;
    private String metricExternalId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Long getLeaderboardId() { return leaderboardId; }
    public void setLeaderboardId(Long leaderboardId) { this.leaderboardId = leaderboardId; }
    public Long getInstanceId() { return instanceId; }
    public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }
    public Long getMetricId() { return metricId; }
    public void setMetricId(Long metricId) { this.metricId = metricId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getMetricName() { return metricName; }
    public void setMetricName(String metricName) { this.metricName = metricName; }
    public String getMetricExternalId() { return metricExternalId; }
    public void setMetricExternalId(String metricExternalId) { this.metricExternalId = metricExternalId; }
}
