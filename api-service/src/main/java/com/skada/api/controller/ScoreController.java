package com.skada.api.controller;

import com.skada.api.model.ScoreRecord;
import com.skada.api.service.ScoreService;
import com.skada.common.model.BaseResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 分数上报接口
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
     */
    @PostMapping("/submit")
    public BaseResponse<ScoreRecord> submit(@RequestBody Map<String, Object> body) {
        String tenantId = (String) body.get("tenantId");
        String secretKey = (String) body.get("secretKey");
        Long leaderboardId = body.get("leaderboardId") != null
                ? ((Number) body.get("leaderboardId")).longValue() : null;
        String userId = (String) body.get("userId");
        BigDecimal score = body.get("score") != null
                ? new BigDecimal(body.get("score").toString()) : null;
        String payload = (String) body.get("payload");

        if (tenantId == null || tenantId.isEmpty()) {
            throw new IllegalArgumentException("tenantId 不能为空");
        }
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalArgumentException("secretKey 不能为空");
        }
        if (leaderboardId == null) {
            throw new IllegalArgumentException("leaderboardId 不能为空");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        if (score == null) {
            throw new IllegalArgumentException("score 不能为空");
        }

        ScoreRecord record = scoreService.submit(tenantId, secretKey, leaderboardId, userId, score, payload);
        return BaseResponse.success(record);
    }

    /**
     * 批量分数上报（同一排行榜）
     */
    @PostMapping("/batch-submit")
    public BaseResponse<Void> batchSubmit(@RequestBody Map<String, Object> body) {
        String tenantId = (String) body.get("tenantId");
        String secretKey = (String) body.get("secretKey");
        Long leaderboardId = body.get("leaderboardId") != null
                ? ((Number) body.get("leaderboardId")).longValue() : null;

        if (tenantId == null || tenantId.isEmpty()) {
            throw new IllegalArgumentException("tenantId 不能为空");
        }
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalArgumentException("secretKey 不能为空");
        }
        if (leaderboardId == null) {
            throw new IllegalArgumentException("leaderboardId 不能为空");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scores = (List<Map<String, Object>>) body.get("scores");
        if (scores == null || scores.isEmpty()) {
            throw new IllegalArgumentException("scores 不能为空");
        }

        List<ScoreService.ScoreSubmitItem> items = scores.stream().map(m -> {
            ScoreService.ScoreSubmitItem item = new ScoreService.ScoreSubmitItem();
            item.setUserId((String) m.get("userId"));
            item.setScore(m.get("score") != null ? new BigDecimal(m.get("score").toString()) : null);
            item.setPayload((String) m.get("payload"));
            if (item.getUserId() == null || item.getScore() == null) {
                throw new IllegalArgumentException("scores 中每条数据必须包含 userId 和 score");
            }
            return item;
        }).toList();

        scoreService.batchSubmit(tenantId, secretKey, leaderboardId, items);
        return BaseResponse.success();
    }
}
