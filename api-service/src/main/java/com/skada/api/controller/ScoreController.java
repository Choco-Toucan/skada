package com.skada.api.controller;

import com.skada.api.model.request.BatchScoreSubmitRequest;
import com.skada.api.model.request.ScoreSubmitRequest;
import com.skada.api.service.ScoreService;
import com.skada.common.model.BaseResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 分数上报接口
 */
@RestController
@RequestMapping("/api/v1/score")
public class ScoreController {

    private static final int BATCH_MAX_SIZE = 1000;

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    /**
     * 单条分数上报
     */
    @PostMapping("/submit")
    public BaseResponse<Void> submit(@RequestBody ScoreSubmitRequest request) {
        validateSubmitRequest(request);

        List<ScoreService.MetricValue> metrics = toServiceMetrics(request.getMetrics());
        scoreService.submit(request.getTenantId(), request.getSecretKey(), request.getUserId(), metrics);
        return BaseResponse.success();
    }

    /**
     * 批量分数上报
     */
    @PostMapping("/batch-submit")
    public BaseResponse<Void> batchSubmit(@RequestBody BatchScoreSubmitRequest request) {
        validateBatchRequest(request);

        List<ScoreService.BatchSubmitItem> items = new ArrayList<>();
        for (var item : request.getScores()) {
            ScoreService.BatchSubmitItem svcItem = new ScoreService.BatchSubmitItem();
            svcItem.setUserId(item.getUserId());
            svcItem.setMetrics(toServiceMetrics(item.getMetrics()));
            items.add(svcItem);
        }

        scoreService.batchSubmit(request.getTenantId(), request.getSecretKey(), items);
        return BaseResponse.success();
    }

    private void validateSubmitRequest(ScoreSubmitRequest request) {
        if (request.getTenantId() == null || request.getTenantId().isBlank()) {
            throw new IllegalArgumentException("tenantId 不能为空");
        }
        if (request.getSecretKey() == null || request.getSecretKey().isBlank()) {
            throw new IllegalArgumentException("secretKey 不能为空");
        }
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        if (request.getMetrics() == null || request.getMetrics().isEmpty()) {
            throw new IllegalArgumentException("metrics 不能为空");
        }
        for (var mv : request.getMetrics()) {
            if (mv.getMetricId() == null || mv.getMetricId().isBlank()) {
                throw new IllegalArgumentException("metrics 中每条数据必须包含 metricId");
            }
            if (mv.getValue() == null) {
                throw new IllegalArgumentException("metrics 中每条数据必须包含 value");
            }
        }
    }

    private void validateBatchRequest(BatchScoreSubmitRequest request) {
        if (request.getTenantId() == null || request.getTenantId().isBlank()) {
            throw new IllegalArgumentException("tenantId 不能为空");
        }
        if (request.getSecretKey() == null || request.getSecretKey().isBlank()) {
            throw new IllegalArgumentException("secretKey 不能为空");
        }
        if (request.getScores() == null || request.getScores().isEmpty()) {
            throw new IllegalArgumentException("scores 不能为空");
        }
        if (request.getScores().size() > BATCH_MAX_SIZE) {
            throw new IllegalArgumentException("单次批量上报不能超过" + BATCH_MAX_SIZE + "条");
        }
        for (var item : request.getScores()) {
            if (item.getUserId() == null || item.getUserId().isBlank()) {
                throw new IllegalArgumentException("scores 中每条数据必须包含 userId");
            }
            if (item.getMetrics() == null || item.getMetrics().isEmpty()) {
                throw new IllegalArgumentException("scores 中每条数据必须包含 metrics");
            }
            for (var mv : item.getMetrics()) {
                if (mv.getMetricId() == null || mv.getMetricId().isBlank()) {
                    throw new IllegalArgumentException("metrics 中每条数据必须包含 metricId");
                }
                if (mv.getValue() == null) {
                    throw new IllegalArgumentException("metrics 中每条数据必须包含 value");
                }
            }
        }
    }

    private List<ScoreService.MetricValue> toServiceMetrics(
            List<ScoreSubmitRequest.MetricValueRequest> requestMetrics) {
        List<ScoreService.MetricValue> result = new ArrayList<>();
        for (var rm : requestMetrics) {
            ScoreService.MetricValue mv = new ScoreService.MetricValue();
            mv.setMetricId(rm.getMetricId());
            mv.setValue(rm.getValue());
            mv.setPayload(rm.getPayload());
            result.add(mv);
        }
        return result;
    }
}
