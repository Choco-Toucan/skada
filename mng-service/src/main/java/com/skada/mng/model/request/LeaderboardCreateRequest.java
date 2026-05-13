package com.skada.mng.model.request;

import java.util.List;

/**
 * 创建排行榜计划请求
 */
public class LeaderboardCreateRequest {

    private String tenantId;
    private String name;
    private Long startTime;
    private Long endTime;
    private Integer maxQueryUsers;
    private Integer allowDuplicateReport;
    private Integer allowHistoryQuery;
    private String rollStrategy;
    private Integer rollIntervalValue;
    private String rollIntervalUnit;
    private Integer rollUserCount;
    private List<MetricAssociation> metrics;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getStartTime() { return startTime; }
    public void setStartTime(Long startTime) { this.startTime = startTime; }
    public Long getEndTime() { return endTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }
    public Integer getMaxQueryUsers() { return maxQueryUsers; }
    public void setMaxQueryUsers(Integer maxQueryUsers) { this.maxQueryUsers = maxQueryUsers; }
    public Integer getAllowDuplicateReport() { return allowDuplicateReport; }
    public void setAllowDuplicateReport(Integer allowDuplicateReport) { this.allowDuplicateReport = allowDuplicateReport; }
    public Integer getAllowHistoryQuery() { return allowHistoryQuery; }
    public void setAllowHistoryQuery(Integer allowHistoryQuery) { this.allowHistoryQuery = allowHistoryQuery; }
    public String getRollStrategy() { return rollStrategy; }
    public void setRollStrategy(String rollStrategy) { this.rollStrategy = rollStrategy; }
    public Integer getRollIntervalValue() { return rollIntervalValue; }
    public void setRollIntervalValue(Integer rollIntervalValue) { this.rollIntervalValue = rollIntervalValue; }
    public String getRollIntervalUnit() { return rollIntervalUnit; }
    public void setRollIntervalUnit(String rollIntervalUnit) { this.rollIntervalUnit = rollIntervalUnit; }
    public Integer getRollUserCount() { return rollUserCount; }
    public void setRollUserCount(Integer rollUserCount) { this.rollUserCount = rollUserCount; }
    public List<MetricAssociation> getMetrics() { return metrics; }
    public void setMetrics(List<MetricAssociation> metrics) { this.metrics = metrics; }

    /**
     * 指标关联定义
     */
    public static class MetricAssociation {
        private Long metricId;
        private Integer priority;
        private String sortOrder;

        public Long getMetricId() { return metricId; }
        public void setMetricId(Long metricId) { this.metricId = metricId; }
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
        public String getSortOrder() { return sortOrder; }
        public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
    }
}
