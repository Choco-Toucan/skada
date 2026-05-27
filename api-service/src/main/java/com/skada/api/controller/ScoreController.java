package com.skada.api.controller;

import com.skada.api.model.request.BatchScoreSubmitRequest;
import com.skada.api.model.request.ScoreSubmitRequest;
import com.skada.api.service.ScoreService;
import com.skada.common.enums.BizCode;
import com.skada.common.exception.BusinessException;
import com.skada.common.model.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 分数上报接口
 * <p>租户身份由 SaasAuthFilter 从 header 校验后注入 request attribute。</p>
 */
@RestController
@RequestMapping("/api/v1/score")
public class ScoreController {

    private static final String ATTR_TENANT_ID = "saasTenantId";
    private static final int BATCH_MAX_SIZE = 1000;

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    /**
     * 单条分数上报
     */
    @PostMapping("/submit")
    public BaseResponse<Void> submit(@RequestBody ScoreSubmitRequest request,
                                     HttpServletRequest httpRequest) {
        String tenantId = requireTenantId(httpRequest);
        validateSubmitRequest(request);

        List<ScoreService.MetricValue> metrics = toServiceMetrics(request.getMetrics());
        scoreService.submit(tenantId, request.getUserId(), metrics);
        return BaseResponse.success();
    }

    /**
     * 批量分数上报
     */
    @PostMapping("/batch-submit")
    public BaseResponse<Void> batchSubmit(@RequestBody BatchScoreSubmitRequest request,
                                          HttpServletRequest httpRequest) {
        String tenantId = requireTenantId(httpRequest);
        validateBatchRequest(request);

        List<ScoreService.BatchSubmitItem> items = new ArrayList<>();
        for (var item : request.getScores()) {
            ScoreService.BatchSubmitItem svcItem = new ScoreService.BatchSubmitItem();
            svcItem.setUserId(item.getUserId());
            svcItem.setMetrics(toServiceMetrics(item.getMetrics()));
            items.add(svcItem);
        }

        scoreService.batchSubmit(tenantId, items);
        return BaseResponse.success();
    }

    /** 从请求中提取已通过Filter校验的租户ID，写入接口强制要求鉴权 */
    private String requireTenantId(HttpServletRequest request) {
        String tenantId = (String) request.getAttribute(ATTR_TENANT_ID);
        if (tenantId == null || tenantId.isBlank()) {
            throw new BusinessException(BizCode.TENANT_AUTH_FAILED, "缺少租户鉴权信息");
        }
        return tenantId;
    }

    private void validateSubmitRequest(ScoreSubmitRequest request) {
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
            if (mv.getMode() != null && !"set".equals(mv.getMode()) && !"inc".equals(mv.getMode())) {
                throw new IllegalArgumentException("mode 仅支持 set 或 inc");
            }
        }
    }

    private void validateBatchRequest(BatchScoreSubmitRequest request) {
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
                if (mv.getMode() != null && !"set".equals(mv.getMode()) && !"inc".equals(mv.getMode())) {
                    throw new IllegalArgumentException("mode 仅支持 set 或 inc");
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
            mv.setMode(rm.getMode());
            result.add(mv);
        }
        return result;
    }
}
