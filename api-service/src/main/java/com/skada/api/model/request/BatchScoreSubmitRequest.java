package com.skada.api.model.request;

import java.util.List;

/**
 * 批量分数上报请求
 * <p>租户身份由 Filter 从 header 校验后注入，body 仅包含业务参数。</p>
 */
public class BatchScoreSubmitRequest {

    private List<BatchItem> scores;

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
