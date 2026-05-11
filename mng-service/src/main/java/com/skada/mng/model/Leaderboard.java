package com.skada.mng.model;

/**
 * 排行榜配置实体
 */
public class Leaderboard {

    private Long id;
    private String tenantId;
    private String name;
    private Long startTime;
    private Long endTime;
    private String sortOrder;
    private Integer maxQueryUsers;
    private Integer allowDuplicateReport;
    private Integer allowHistoryQuery;
    private String rollStrategy;
    private Integer rollIntervalValue;
    private String rollIntervalUnit;
    private Integer rollUserCount;
    private String status;
    private Long currentCycleId;
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
    public Long getStartTime() { return startTime; }
    public void setStartTime(Long startTime) { this.startTime = startTime; }
    public Long getEndTime() { return endTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }
    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCurrentCycleId() { return currentCycleId; }
    public void setCurrentCycleId(Long currentCycleId) { this.currentCycleId = currentCycleId; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
}
