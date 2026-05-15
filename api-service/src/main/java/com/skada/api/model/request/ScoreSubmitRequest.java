package com.skada.api.model.request;

import java.math.BigDecimal;
import java.util.List;

/**
 * 单条分数上报请求
 */
public class ScoreSubmitRequest {

    private String tenantId;
    private String secretKey;
    private String userId;
    private List<MetricValueRequest> metrics;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
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
