package com.skada.api.controller;

import com.skada.api.model.LeaderboardInstance;
import com.skada.api.service.LeaderboardQueryService;
import com.skada.common.model.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
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

    public LeaderboardController(LeaderboardQueryService queryService) {
        this.queryService = queryService;
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
}
