package com.skada.api.model.request;

import java.math.BigDecimal;
import java.util.List;

/**
 * 单条分数上报请求
 * <p>租户身份由 Filter 从 header 校验后注入，body 仅包含业务参数。</p>
 */
public class ScoreSubmitRequest {

    private String userId;
    private List<MetricValueRequest> metrics;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<MetricValueRequest> getMetrics() { return metrics; }
    public void setMetrics(List<MetricValueRequest> metrics) { this.metrics = metrics; }

    public static class MetricValueRequest {
        private String metricId;
        private BigDecimal value;
        private String payload;

        public String getMetricId() { return metricId; }
        public void setMetricId(String metricId) { this.metricId = metricId; }
        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal value) { this.value = value; }
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
    }
}
