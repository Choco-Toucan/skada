package com.skada.mng.service;

import com.skada.common.exception.BusinessException;
import com.skada.common.model.PageResult;
import com.skada.common.util.DistributedLock;
import com.skada.mng.mapper.LeaderboardInstanceMapper;
import com.skada.mng.mapper.LeaderboardMapper;
import com.skada.mng.mapper.LeaderboardMetricMapper;
import com.skada.mng.model.Leaderboard;
import com.skada.mng.model.LeaderboardInstance;
import com.skada.mng.model.LeaderboardMetric;
import com.skada.mng.model.request.LeaderboardCreateRequest;
import com.skada.mng.model.request.LeaderboardUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 排行榜配置服务
 * 负责排行榜的创建、配置、滚动和终止
 */
@Service
public class LeaderboardConfigService {

    private final LeaderboardMapper leaderboardMapper;
    private final LeaderboardInstanceMapper instanceMapper;
    private final LeaderboardMetricMapper leaderboardMetricMapper;
    private final TenantService tenantService;
    private final DistributedLock distributedLock;

    public LeaderboardConfigService(LeaderboardMapper leaderboardMapper,
                                    LeaderboardInstanceMapper instanceMapper,
                                    LeaderboardMetricMapper leaderboardMetricMapper,
                                    TenantService tenantService,
                                    DistributedLock distributedLock) {
        this.leaderboardMapper = leaderboardMapper;
        this.instanceMapper = instanceMapper;
        this.leaderboardMetricMapper = leaderboardMetricMapper;
        this.tenantService = tenantService;
        this.distributedLock = distributedLock;
    }

    /**
     * 创建排行榜
     * 同时创建第一个活跃实例
     */
    @Transactional
    public Leaderboard create(LeaderboardCreateRequest request, String adminId) {
        // 校验租户存在
        if (tenantService.findByTenantId(request.getTenantId()) == null) {
            throw new BusinessException("租户不存在");
        }
        if (request.getMetrics() == null || request.getMetrics().isEmpty()) {
            throw new BusinessException("至少需要关联一个指标");
        }

        validateCreateRequest(request);

        Leaderboard lb = new Leaderboard();
        lb.setPlanId("lb_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        lb.setTenantId(request.getTenantId());
        lb.setName(request.getName().trim());
        lb.setStartTime(request.getStartTime());
        lb.setEndTime(request.getEndTime());
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

        // 创建第一个活跃实例
        LeaderboardInstance instance = new LeaderboardInstance();
        instance.setInstanceId("li_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        instance.setLeaderboardId(lb.getId());
        instance.setInstanceSeq(1);
        instance.setStartTime(request.getStartTime());
        instance.setStatus("active");
        instance.setCreateBy(adminId);
        instance.setUpdateBy(adminId);
        instanceMapper.insert(instance);

        // 更新排行榜的当前实例ID
        lb.setCurrentInstanceId(instance.getId());
        leaderboardMapper.update(lb);

        // 关联指标
        List<LeaderboardMetric> metrics = new ArrayList<>();
        for (var ma : request.getMetrics()) {
            LeaderboardMetric lm = new LeaderboardMetric();
            lm.setLeaderboardId(lb.getId());
            lm.setMetricId(ma.getMetricId());
            lm.setPriority(ma.getPriority() != null ? ma.getPriority() : 1);
            lm.setSortOrder(ma.getSortOrder() != null ? ma.getSortOrder() : "desc");
            metrics.add(lm);
        }
        leaderboardMetricMapper.insertBatch(metrics);

        return lb;
    }

    /**
     * 更新排行榜配置
     */
    @Transactional
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
        if (request.getMaxQueryUsers() != null) lb.setMaxQueryUsers(request.getMaxQueryUsers());
        if (request.getAllowDuplicateReport() != null) lb.setAllowDuplicateReport(request.getAllowDuplicateReport());
        if (request.getAllowHistoryQuery() != null) lb.setAllowHistoryQuery(request.getAllowHistoryQuery());
        if (request.getRollStrategy() != null) lb.setRollStrategy(request.getRollStrategy());
        if (request.getRollIntervalValue() != null) lb.setRollIntervalValue(request.getRollIntervalValue());
        if (request.getRollIntervalUnit() != null) lb.setRollIntervalUnit(request.getRollIntervalUnit());
        if (request.getRollUserCount() != null) lb.setRollUserCount(request.getRollUserCount());
        lb.setUpdateBy(adminId);

        leaderboardMapper.update(lb);

        // 更新指标关联：先删后插
        if (request.getMetrics() != null && !request.getMetrics().isEmpty()) {
            leaderboardMetricMapper.deleteByLeaderboardId(lb.getId());
            List<LeaderboardMetric> metrics = new ArrayList<>();
            for (var ma : request.getMetrics()) {
                LeaderboardMetric lm = new LeaderboardMetric();
                lm.setLeaderboardId(lb.getId());
                lm.setMetricId(ma.getMetricId());
                lm.setPriority(ma.getPriority() != null ? ma.getPriority() : 1);
                lm.setSortOrder(ma.getSortOrder() != null ? ma.getSortOrder() : "desc");
                metrics.add(lm);
            }
            leaderboardMetricMapper.insertBatch(metrics);
        }

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
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;
        if (pageSize > 100) pageSize = 100;
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

        String lockValue = UUID.randomUUID().toString();
        String lockName = "roll:leaderboard:" + leaderboardId;
        if (!distributedLock.tryLock(lockName, lockValue, 10, TimeUnit.SECONDS)) {
            throw new BusinessException("排行榜正在滚动中，请稍后重试");
        }

        try {
            // 再次校验活跃实例（双重检查，防止锁获取期间实例已被关闭）
            LeaderboardInstance activeInstance = instanceMapper.findActiveByLeaderboardId(leaderboardId);
            if (activeInstance == null) {
                throw new BusinessException("当前没有活跃实例，无需滚动");
            }

            instanceMapper.closeInstance(activeInstance.getId(), System.currentTimeMillis());

            int maxSeq = instanceMapper.getMaxInstanceSeq(leaderboardId);
            LeaderboardInstance newInstance = new LeaderboardInstance();
            newInstance.setInstanceId("li_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
            newInstance.setLeaderboardId(leaderboardId);
            newInstance.setInstanceSeq(maxSeq + 1);
            newInstance.setStartTime(System.currentTimeMillis());
            newInstance.setStatus("active");
            newInstance.setCreateBy(adminId);
            newInstance.setUpdateBy(adminId);
            instanceMapper.insert(newInstance);

            lb.setCurrentInstanceId(newInstance.getId());
            leaderboardMapper.update(lb);

            return lb;
        } finally {
            distributedLock.unlock(lockName, lockValue);
        }
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

        String lockValue = UUID.randomUUID().toString();
        String lockName = "roll:leaderboard:" + leaderboardId;
        if (!distributedLock.tryLock(lockName, lockValue, 10, TimeUnit.SECONDS)) {
            throw new BusinessException("排行榜操作进行中，请稍后重试");
        }

        try {
            // 关闭当前活跃实例
            LeaderboardInstance activeInstance = instanceMapper.findActiveByLeaderboardId(leaderboardId);
            if (activeInstance != null) {
                instanceMapper.closeInstance(activeInstance.getId(), System.currentTimeMillis());
            }

            lb.setStatus("stopped");
            lb.setUpdateBy(adminId);
            leaderboardMapper.update(lb);

            return lb;
        } finally {
            distributedLock.unlock(lockName, lockValue);
        }
    }

    /**
     * 查询排行榜的所有实例（含历史实例）
     */
    public List<LeaderboardInstance> getInstances(Long leaderboardId) {
        return instanceMapper.findByLeaderboardId(leaderboardId);
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
