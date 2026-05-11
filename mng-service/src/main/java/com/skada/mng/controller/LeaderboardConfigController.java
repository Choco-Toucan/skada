package com.skada.mng.controller;

import com.skada.common.annotation.RequirePermission;
import com.skada.common.model.BaseResponse;
import com.skada.common.model.PageResult;
import com.skada.mng.model.Leaderboard;
import com.skada.mng.model.LeaderboardCycle;
import com.skada.mng.model.request.LeaderboardCreateRequest;
import com.skada.mng.model.request.LeaderboardUpdateRequest;
import com.skada.mng.service.LeaderboardConfigService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 排行榜配置接口
 */
@RestController
@RequestMapping("/api/v1/leaderboard")
public class LeaderboardConfigController {

    private final LeaderboardConfigService configService;

    public LeaderboardConfigController(LeaderboardConfigService configService) {
        this.configService = configService;
    }

    @RequirePermission("admin")
    @PostMapping("/create")
    public BaseResponse<Leaderboard> create(@RequestBody LeaderboardCreateRequest request,
                                            HttpServletRequest httpRequest) {
        String adminId = (String) httpRequest.getAttribute("adminId");
        return BaseResponse.success(configService.create(request, adminId));
    }

    @RequirePermission("admin")
    @PostMapping("/update")
    public BaseResponse<Leaderboard> update(@RequestBody LeaderboardUpdateRequest request,
                                            HttpServletRequest httpRequest) {
        String adminId = (String) httpRequest.getAttribute("adminId");
        return BaseResponse.success(configService.update(request, adminId));
    }

    @GetMapping("/list")
    public BaseResponse<PageResult<Leaderboard>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String tenantId) {
        return BaseResponse.success(configService.findAllWithPage(page, pageSize, tenantId));
    }

    @GetMapping("/get")
    public BaseResponse<Leaderboard> get(Long id) {
        return BaseResponse.success(configService.findById(id));
    }

    @RequirePermission("admin")
    @PostMapping("/roll")
    public BaseResponse<Leaderboard> roll(@RequestBody java.util.Map<String, Long> request,
                                          HttpServletRequest httpRequest) {
        Long leaderboardId = request.get("leaderboardId");
        if (leaderboardId == null) {
            throw new IllegalArgumentException("排行榜ID不能为空");
        }
        String adminId = (String) httpRequest.getAttribute("adminId");
        return BaseResponse.success(configService.roll(leaderboardId, adminId));
    }

    @RequirePermission("admin")
    @PostMapping("/stop")
    public BaseResponse<Leaderboard> stop(@RequestBody java.util.Map<String, Long> request,
                                          HttpServletRequest httpRequest) {
        Long leaderboardId = request.get("leaderboardId");
        if (leaderboardId == null) {
            throw new IllegalArgumentException("排行榜ID不能为空");
        }
        String adminId = (String) httpRequest.getAttribute("adminId");
        return BaseResponse.success(configService.stop(leaderboardId, adminId));
    }

    /**
     * 查询排行榜的所有周期（含历史周期），供管理后台查看
     */
    @GetMapping("/cycles")
    public BaseResponse<List<LeaderboardCycle>> getCycles(Long leaderboardId) {
        return BaseResponse.success(configService.getCycles(leaderboardId));
    }
}
