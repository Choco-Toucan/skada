package com.skada.api.model.request;

import java.util.List;

/**
 * 批量分数上报请求
 */
public class BatchScoreSubmitRequest {

    private String tenantId;
    private String secretKey;
    private List<BatchItem> scores;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public List<BatchItem> getScores() { return scores; }
    public void setScores(List<BatchItem> scores) { this.scores = scores; }

    public static class BatchItem {
        private String userId;
        private List<ScoreSubmitRequest.MetricValueRequest> metrics;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public List<ScoreSubmitRequest.MetricValueRequest> getMetrics() { return metrics; }
        public void setMetrics(List<ScoreSubmitRequest.MetricValueRequest> metrics) { this.metrics = metrics; }
    }
}
