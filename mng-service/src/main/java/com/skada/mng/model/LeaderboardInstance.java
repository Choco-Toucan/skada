package com.skada.mng.model;

/**
 * 排行榜实例实体
 * 排行榜计划的具体执行实例，每次滚动产生一个新实例
 */
public class LeaderboardInstance {

    private Long id;
    private String instanceId;
    private Long leaderboardId;
    private Integer instanceSeq;
    private Long startTime;
    private Long endTime;
    private String status;
    private String createTime;
    private String updateTime;
    private String createBy;
    private String updateBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
    public Long getLeaderboardId() { return leaderboardId; }
    public void setLeaderboardId(Long leaderboardId) { this.leaderboardId = leaderboardId; }
    public Integer getInstanceSeq() { return instanceSeq; }
    public void setInstanceSeq(Integer instanceSeq) { this.instanceSeq = instanceSeq; }
    public Long getStartTime() { return startTime; }
    public void setStartTime(Long startTime) { this.startTime = startTime; }
    public Long getEndTime() { return endTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
}
