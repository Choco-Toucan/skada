package com.skada.mng.model;

/**
 * 排行榜计划实体
 */
public class Leaderboard {

    private Long id;
    private String planId;
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
    private String status;
    private Long currentInstanceId;
    private String currentInstanceBusinessId;
    private String createTime;
    private String updateTime;
    private String createBy;
    private String updateBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getStartTime() { return startTime; }
    public void setStartTime(Long startTime) { this.startTime = startTime; }
    public Long getEndTime() { return endTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }
    public Integer getMaxQueryUsers() { return maxQueryUsers != null ? maxQueryUsers : 1000; }
    public void setMaxQueryUsers(Integer maxQueryUsers) { this.maxQueryUsers = maxQueryUsers; }
    public Integer getAllowDuplicateReport() { return allowDuplicateReport != null ? allowDuplicateReport : 0; }
    public void setAllowDuplicateReport(Integer allowDuplicateReport) { this.allowDuplicateReport = allowDuplicateReport; }
    public Integer getAllowHistoryQuery() { return allowHistoryQuery != null ? allowHistoryQuery : 1; }
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
    public Long getCurrentInstanceId() { return currentInstanceId; }
    public void setCurrentInstanceId(Long currentInstanceId) { this.currentInstanceId = currentInstanceId; }
    public String getCurrentInstanceBusinessId() { return currentInstanceBusinessId; }
    public void setCurrentInstanceBusinessId(String currentInstanceBusinessId) { this.currentInstanceBusinessId = currentInstanceBusinessId; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
}
