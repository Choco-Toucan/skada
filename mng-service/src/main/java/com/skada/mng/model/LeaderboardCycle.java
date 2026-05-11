package com.skada.mng.model;

/**
 * 排行榜周期实体
 */
public class LeaderboardCycle {

    private Long id;
    private Long leaderboardId;
    private Integer cycleSeq;
    private Long cycleStartTime;
    private Long cycleEndTime;
    private String status;
    private String createTime;
    private String updateTime;
    private String createBy;
    private String updateBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLeaderboardId() { return leaderboardId; }
    public void setLeaderboardId(Long leaderboardId) { this.leaderboardId = leaderboardId; }
    public Integer getCycleSeq() { return cycleSeq; }
    public void setCycleSeq(Integer cycleSeq) { this.cycleSeq = cycleSeq; }
    public Long getCycleStartTime() { return cycleStartTime; }
    public void setCycleStartTime(Long cycleStartTime) { this.cycleStartTime = cycleStartTime; }
    public Long getCycleEndTime() { return cycleEndTime; }
    public void setCycleEndTime(Long cycleEndTime) { this.cycleEndTime = cycleEndTime; }
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
