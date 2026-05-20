package com.skada.api.model;

/**
 * 排行榜关联指标（API服务用）
 */
public class LeaderboardMetric {

    private Long id;
    private Long leaderboardId;
    private Long metricId;
    private Integer priority;
    private String sortOrder;
    private String createBy;
    private String updateBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLeaderboardId() { return leaderboardId; }
    public void setLeaderboardId(Long leaderboardId) { this.leaderboardId = leaderboardId; }
    public Long getMetricId() { return metricId; }
    public void setMetricId(Long metricId) { this.metricId = metricId; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
}
