package com.skada.mng.service;

import com.skada.common.exception.BusinessException;
import com.skada.common.model.PageResult;
import com.skada.mng.mapper.LeaderboardCycleMapper;
import com.skada.mng.mapper.LeaderboardMapper;
import com.skada.mng.model.Leaderboard;
import com.skada.mng.model.LeaderboardCycle;
import com.skada.mng.model.request.LeaderboardCreateRequest;
import com.skada.mng.model.request.LeaderboardUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 排行榜配置服务
 * 负责排行榜的创建、配置、滚动和终止
 */
@Service
public class LeaderboardConfigService {

    private final LeaderboardMapper leaderboardMapper;
    private final LeaderboardCycleMapper cycleMapper;
    private final TenantService tenantService;

    public LeaderboardConfigService(LeaderboardMapper leaderboardMapper,
                                    LeaderboardCycleMapper cycleMapper,
                                    TenantService tenantService) {
        this.leaderboardMapper = leaderboardMapper;
        this.cycleMapper = cycleMapper;
        this.tenantService = tenantService;
    }

    /**
     * 创建排行榜
     * 同时创建第一个活跃周期
     */
    @Transactional
    public Leaderboard create(LeaderboardCreateRequest request, String adminId) {
        // 校验租户存在
        if (tenantService.findByTenantId(request.getTenantId()) == null) {
            throw new BusinessException("租户不存在");
        }

        // 校验参数
        validateCreateRequest(request);

        Leaderboard lb = new Leaderboard();
        lb.setTenantId(request.getTenantId());
        lb.setName(request.getName().trim());
        lb.setStartTime(request.getStartTime());
        lb.setEndTime(request.getEndTime());
        lb.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : "desc");
        lb.setMaxQueryUsers(request.getMaxQueryUsers() != null ? request.getMaxQueryUsers() : 1000);
        lb.setAllowDuplicateReport(request.getAllowDuplicateReport() != null ? request.getAllowDuplicateReport() : 0);
        lb.setAllowHistoryQuery(request.getAllowHistoryQuery() != null ? request.getAllowHistoryQuery() : 1);
        lb.setRollStrategy(request.getRollStrategy() != null ? request.getRollStrategy() : "none");
        lb.setRollIntervalValue(request.getRollIntervalValue());
        lb.setRollIntervalUnit(request.getRollIntervalUnit());
        lb.setRollUserCount(request.getRollUserCount());
        lb.setStatus("active");
        lb.setCreateBy(adminId);
        lb.setUpdateBy(adminId);

        leaderboardMapper.insert(lb);

        // 创建第一个活跃周期
        LeaderboardCycle cycle = new LeaderboardCycle();
        cycle.setLeaderboardId(lb.getId());
        cycle.setCycleSeq(1);
        cycle.setCycleStartTime(request.getStartTime());
        cycle.setStatus("active");
        cycle.setCreateBy(adminId);
        cycle.setUpdateBy(adminId);
        cycleMapper.insert(cycle);

        // 更新排行榜的当前周期ID
        lb.setCurrentCycleId(cycle.getId());
        leaderboardMapper.update(lb);

        return lb;
    }

    /**
     * 更新排行榜配置
     */
    public Leaderboard update(LeaderboardUpdateRequest request, String adminId) {
        Leaderboard lb = leaderboardMapper.findById(request.getId());
        if (lb == null) {
            throw new BusinessException("排行榜不存在");
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            lb.setName(request.getName().trim());
        }
        if (request.getStartTime() != null) lb.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) lb.setEndTime(request.getEndTime());
        if (request.getSortOrder() != null) lb.setSortOrder(request.getSortOrder());
        if (request.getMaxQueryUsers() != null) lb.setMaxQueryUsers(request.getMaxQueryUsers());
        if (request.getAllowDuplicateReport() != null) lb.setAllowDuplicateReport(request.getAllowDuplicateReport());
        if (request.getAllowHistoryQuery() != null) lb.setAllowHistoryQuery(request.getAllowHistoryQuery());
        if (request.getRollStrategy() != null) lb.setRollStrategy(request.getRollStrategy());
        if (request.getRollIntervalValue() != null) lb.setRollIntervalValue(request.getRollIntervalValue());
        if (request.getRollIntervalUnit() != null) lb.setRollIntervalUnit(request.getRollIntervalUnit());
        if (request.getRollUserCount() != null) lb.setRollUserCount(request.getRollUserCount());
        lb.setUpdateBy(adminId);

        leaderboardMapper.update(lb);
        return leaderboardMapper.findById(lb.getId());
    }

    /**
     * 查询指定租户下的所有排行榜
     */
    public List<Leaderboard> findByTenantId(String tenantId) {
        return leaderboardMapper.findByTenantId(tenantId);
    }

    /**
     * 分页查询排行榜（可按租户筛选）
     */
    public PageResult<Leaderboard> findAllWithPage(int page, int pageSize, String tenantId) {
        int offset = (page - 1) * pageSize;
        if (tenantId != null && !tenantId.isEmpty()) {
            List<Leaderboard> records = leaderboardMapper.findByTenantIdWithPage(tenantId, offset, pageSize);
            long total = leaderboardMapper.countByTenantId(tenantId);
            return new PageResult<>(records, total, page, pageSize);
        }
        List<Leaderboard> records = leaderboardMapper.findAllWithPage(offset, pageSize);
        long total = leaderboardMapper.count();
        return new PageResult<>(records, total, page, pageSize);
    }

    /**
     * 查询所有排行榜
     */
    public List<Leaderboard> findAll() {
        return leaderboardMapper.findAll();
    }

    /**
     * 根据ID查询排行榜
     */
    public Leaderboard findById(Long id) {
        Leaderboard lb = leaderboardMapper.findById(id);
        if (lb == null) {
            throw new BusinessException("排行榜不存在");
        }
        return lb;
    }

    /**
     * 手动触发滚动
     */
    @Transactional
    public Leaderboard roll(Long leaderboardId, String adminId) {
        Leaderboard lb = leaderboardMapper.findById(leaderboardId);
        if (lb == null) {
            throw new BusinessException("排行榜不存在");
        }
        if (!"active".equals(lb.getStatus())) {
            throw new BusinessException("排行榜已终止，无法滚动");
        }

        // 关闭当前活跃周期
        LeaderboardCycle activeCycle = cycleMapper.findActiveByLeaderboardId(leaderboardId);
        if (activeCycle != null) {
            cycleMapper.closeCycle(activeCycle.getId(), System.currentTimeMillis());
        }

        // 创建新周期
        int maxSeq = cycleMapper.getMaxCycleSeq(leaderboardId);
        LeaderboardCycle newCycle = new LeaderboardCycle();
        newCycle.setLeaderboardId(leaderboardId);
        newCycle.setCycleSeq(maxSeq + 1);
        newCycle.setCycleStartTime(System.currentTimeMillis());
        newCycle.setStatus("active");
        newCycle.setCreateBy(adminId);
        newCycle.setUpdateBy(adminId);
        cycleMapper.insert(newCycle);

        // 更新排行榜的当前周期
        lb.setCurrentCycleId(newCycle.getId());
        leaderboardMapper.update(lb);

        return lb;
    }

    /**
     * 手动终止排行榜
     */
    @Transactional
    public Leaderboard stop(Long leaderboardId, String adminId) {
        Leaderboard lb = leaderboardMapper.findById(leaderboardId);
        if (lb == null) {
            throw new BusinessException("排行榜不存在");
        }
        if (!"active".equals(lb.getStatus())) {
            throw new BusinessException("排行榜已经处于终止状态");
        }

        // 关闭当前活跃周期
        LeaderboardCycle activeCycle = cycleMapper.findActiveByLeaderboardId(leaderboardId);
        if (activeCycle != null) {
            cycleMapper.closeCycle(activeCycle.getId(), System.currentTimeMillis());
        }

        // 标记排行榜已终止
        lb.setStatus("stopped");
        lb.setUpdateBy(adminId);
        leaderboardMapper.update(lb);

        return lb;
    }

    /**
     * 查询排行榜的所有周期（含历史周期）
     */
    public List<LeaderboardCycle> getCycles(Long leaderboardId) {
        return cycleMapper.findByLeaderboardId(leaderboardId);
    }

    private void validateCreateRequest(LeaderboardCreateRequest request) {
        if (request.getTenantId() == null || request.getTenantId().isBlank()) {
            throw new BusinessException("租户ID不能为空");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException("排行榜名称不能为空");
        }
        if (request.getStartTime() == null) {
            throw new BusinessException("开始时间不能为空");
        }
        if (request.getEndTime() != null && request.getEndTime() <= request.getStartTime()) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }

        // 校验滚动策略参数
        if ("periodic".equals(request.getRollStrategy())) {
            if (request.getRollIntervalValue() == null || request.getRollIntervalValue() <= 0) {
                throw new BusinessException("周期性滚动必须指定间隔值");
            }
            if (request.getRollIntervalUnit() == null
                    || (!"minute".equals(request.getRollIntervalUnit())
                    && !"hour".equals(request.getRollIntervalUnit())
                    && !"day".equals(request.getRollIntervalUnit()))) {
                throw new BusinessException("周期性滚动的时间单位必须为 minute/hour/day");
            }
        }
        if ("user_count".equals(request.getRollStrategy())) {
            if (request.getRollUserCount() == null || request.getRollUserCount() <= 0) {
                throw new BusinessException("按用户数滚动必须指定用户数阈值");
            }
        }
    }
}
