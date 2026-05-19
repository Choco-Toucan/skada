package com.skada.api.controller;

import com.skada.api.model.LeaderboardInstance;
import com.skada.api.model.request.RollLeaderboardRequest;
import com.skada.api.model.response.RollLeaderboardResponse;
import com.skada.api.service.LeaderboardQueryService;
import com.skada.api.service.LeaderboardRollService;
import com.skada.common.model.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 排行榜查询接口
 */
@RestController
@RequestMapping("/api/v1/leaderboard")
public class LeaderboardController {

    private final LeaderboardQueryService queryService;
    private final LeaderboardRollService rollService;

    public LeaderboardController(LeaderboardQueryService queryService,
                                 LeaderboardRollService rollService) {
        this.queryService = queryService;
        this.rollService = rollService;
    }

    /**
     * 查询排行榜排名（多指标）
     * @param planId 排行榜计划外部ID
     * @param instanceId 实例外部ID（可选，默认当前活跃实例）
     * @param from 起始位置（0-based，含）
     * @param to 结束位置（0-based，含），范围受maxQueryUsers约束
     */
    @GetMapping("/ranking")
    public BaseResponse<List<LeaderboardQueryService.RankEntry>> getRanking(
            @RequestParam String planId,
            @RequestParam(required = false) String instanceId,
            @RequestParam int from,
            @RequestParam int to,
            HttpServletRequest httpRequest) {
        String tenantId = (String) httpRequest.getAttribute("saasTenantId");
        List<LeaderboardQueryService.RankEntry> ranking =
                queryService.getRanking(planId, instanceId, from, to, tenantId);
        return BaseResponse.success(ranking);
    }

    /**
     * 查询排行榜计划的所有实例列表（含历史）
     */
    @GetMapping("/instances")
    public BaseResponse<List<LeaderboardInstance>> getInstances(@RequestParam String planId) {
        return BaseResponse.success(queryService.getInstances(planId));
    }

    /**
     * 租户通过API手动触发排行榜滚动
     */
    @PostMapping("/roll")
    public BaseResponse<RollLeaderboardResponse> roll(@RequestBody RollLeaderboardRequest request) {
        if (request.getTenantId() == null || request.getTenantId().isBlank()) {
            throw new IllegalArgumentException("tenantId 不能为空");
        }
        if (request.getSecretKey() == null || request.getSecretKey().isBlank()) {
            throw new IllegalArgumentException("secretKey 不能为空");
        }
        if (request.getPlanId() == null || request.getPlanId().isBlank()) {
            throw new IllegalArgumentException("planId 不能为空");
        }
        if (request.getInstanceId() == null) {
            throw new IllegalArgumentException("instanceId 不能为null");
        }
        return BaseResponse.success(rollService.roll(request));
    }
}
