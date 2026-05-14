package com.skada.api.controller;

import com.skada.api.service.ScoreService;
import com.skada.common.model.BaseResponse;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 分数上报接口
 * payload 位于指标+分数层级：每个 metric 可携带独立的 payload
 */
@RestController
@RequestMapping("/api/v1/score")
public class ScoreController {

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    /**
     * 单条分数上报
     * 请求体: {tenantId, secretKey, userId, metrics: [{metricId(字符串), value, payload?}]}
     */
    @PostMapping("/submit")
    public BaseResponse<Void> submit(@RequestBody Map<String, Object> body) {
        String tenantId = requireString(body, "tenantId");
        String secretKey = requireString(body, "secretKey");
        String userId = requireString(body, "userId");

        List<ScoreService.MetricValue> metrics = parseMetrics(body);
        if (metrics.isEmpty()) {
            throw new IllegalArgumentException("metrics 不能为空");
        }

        scoreService.submit(tenantId, secretKey, userId, metrics);
        return BaseResponse.success();
    }

    /**
     * 批量分数上报
     * 请求体: {tenantId, secretKey, scores: [{userId, metrics: [{metricId(字符串), value, payload?}]}]}
     */
    @PostMapping("/batch-submit")
    public BaseResponse<Void> batchSubmit(@RequestBody Map<String, Object> body) {
        String tenantId = requireString(body, "tenantId");
        String secretKey = requireString(body, "secretKey");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scores = (List<Map<String, Object>>) body.get("scores");
        if (scores == null || scores.isEmpty()) {
            throw new IllegalArgumentException("scores 不能为空");
        }

        List<ScoreService.BatchSubmitItem> items = new ArrayList<>();
        for (Map<String, Object> m : scores) {
            ScoreService.BatchSubmitItem item = new ScoreService.BatchSubmitItem();
            String uid = (String) m.get("userId");
            if (uid == null || uid.isBlank()) {
                throw new IllegalArgumentException("scores 中每条数据必须包含 userId");
            }
            item.setUserId(uid);

            List<ScoreService.MetricValue> metrics = parseMetricsFromMap(m);
            if (metrics.isEmpty()) {
                throw new IllegalArgumentException("scores 中每条数据必须包含 metrics");
            }
            item.setMetrics(metrics);
            items.add(item);
        }

        scoreService.batchSubmit(tenantId, secretKey, items);
        return BaseResponse.success();
    }

    @SuppressWarnings("unchecked")
    private List<ScoreService.MetricValue> parseMetrics(Map<String, Object> body) {
        return parseMetricsFromMap(body);
    }

    @SuppressWarnings("unchecked")
    private List<ScoreService.MetricValue> parseMetricsFromMap(Map<String, Object> map) {
        List<Map<String, Object>> raw = (List<Map<String, Object>>) map.get("metrics");
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<ScoreService.MetricValue> result = new ArrayList<>();
        for (Map<String, Object> m : raw) {
            ScoreService.MetricValue mv = new ScoreService.MetricValue();
            Object rawMetricId = m.get("metricId");
            mv.setMetricId(rawMetricId != null ? rawMetricId.toString() : null);
            mv.setValue(m.get("value") != null ? new BigDecimal(m.get("value").toString()) : null);
            mv.setPayload((String) m.get("payload"));
            if (mv.getMetricId() == null || mv.getValue() == null) {
                throw new IllegalArgumentException("metrics 中每条数据必须包含 metricId 和 value");
            }
            result.add(mv);
        }
        return result;
    }

    private String requireString(Map<String, Object> body, String key) {
        String val = (String) body.get(key);
        if (val == null || val.isBlank()) {
            throw new IllegalArgumentException(key + " 不能为空");
        }
        return val;
    }
}
