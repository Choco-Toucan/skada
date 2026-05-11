package com.skada.api.controller;

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
     * 查询排行榜排名
     * @param leaderboardId 排行榜ID
     * @param cycleId 周期ID（可选，默认当前活跃周期）
     * @param limit 返回条数（可选，默认100，不超过排行榜配置的maxQueryUsers）
     */
    @GetMapping("/ranking")
    public BaseResponse<List<LeaderboardQueryService.RankEntry>> getRanking(
            @RequestParam Long leaderboardId,
            @RequestParam(required = false) Long cycleId,
            @RequestParam(defaultValue = "100") int limit) {
        List<LeaderboardQueryService.RankEntry> ranking =
                queryService.getRanking(leaderboardId, cycleId, limit);
        return BaseResponse.success(ranking);
    }
}
