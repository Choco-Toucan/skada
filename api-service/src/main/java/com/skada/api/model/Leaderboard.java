package com.skada.api.model;

/**
 * 排行榜计划（API服务用）
 */
public class Leaderboard {

    private Long id;
    private String tenantId;
    private String name;
    private Long startTime;
    private Long endTime;
    private Integer maxQueryUsers;
    private Integer allowDuplicateReport;
    private Integer allowHistoryQuery;
    private String rollStrategy;
    private Integer rollUserCount;
    private String status;
    private Long currentInstanceId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public Integer getRollUserCount() { return rollUserCount; }
    public void setRollUserCount(Integer rollUserCount) { this.rollUserCount = rollUserCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCurrentInstanceId() { return currentInstanceId; }
    public void setCurrentInstanceId(Long currentInstanceId) { this.currentInstanceId = currentInstanceId; }
}
