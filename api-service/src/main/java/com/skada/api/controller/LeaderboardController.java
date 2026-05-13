package com.skada.api.controller;

import com.skada.api.model.LeaderboardInstance;
import com.skada.api.service.LeaderboardQueryService;
import com.skada.common.model.BaseResponse;
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
     * @param leaderboardId 排行榜计划ID
     * @param instanceId 实例ID（可选，默认当前活跃实例）
     * @param limit 返回条数（可选，默认100）
     * @param tenantId 租户ID（可选，仅当租户不允许匿名查询时必填）
     * @param secretKey 租户密钥（可选，仅当租户不允许匿名查询时必填）
     */
    @GetMapping("/ranking")
    public BaseResponse<List<LeaderboardQueryService.RankEntry>> getRanking(
            @RequestParam Long leaderboardId,
            @RequestParam(required = false) Long instanceId,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String secretKey) {
        List<LeaderboardQueryService.RankEntry> ranking =
                queryService.getRanking(leaderboardId, instanceId, limit, tenantId, secretKey);
        return BaseResponse.success(ranking);
    }

    /**
     * 查询排行榜计划的所有实例列表（含历史）
     */
    @GetMapping("/instances")
    public BaseResponse<List<LeaderboardInstance>> getInstances(@RequestParam Long leaderboardId) {
        return BaseResponse.success(queryService.getInstances(leaderboardId));
    }
}
