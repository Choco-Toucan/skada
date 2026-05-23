package com.skada.mng.model.response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 排行榜排名条目（管理后台查看用）
 */
public class LeaderboardRankEntry {

    private int rank;
    private String userId;
    private List<MetricValueEntry> metricValues = new ArrayList<>();

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<MetricValueEntry> getMetricValues() { return metricValues; }
    public void setMetricValues(List<MetricValueEntry> metricValues) { this.metricValues = metricValues; }

    public static class MetricValueEntry {
        private String metricId;
        private String metricName;
        private BigDecimal value;
        private String payload;

        public String getMetricId() { return metricId; }
        public void setMetricId(String metricId) { this.metricId = metricId; }
        public String getMetricName() { return metricName; }
        public void setMetricName(String metricName) { this.metricName = metricName; }
        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal value) { this.value = value; }
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
    }
}
