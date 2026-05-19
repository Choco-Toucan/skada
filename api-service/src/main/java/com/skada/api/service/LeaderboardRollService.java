package com.skada.api.service;

import com.skada.api.mapper.LeaderboardInstanceMapper;
import com.skada.api.mapper.LeaderboardMapper;
import com.skada.api.model.Leaderboard;
import com.skada.api.model.LeaderboardInstance;
import com.skada.api.model.Tenant;
import com.skada.api.model.request.RollLeaderboardRequest;
import com.skada.api.model.response.RollLeaderboardResponse;
import com.skada.common.exception.BusinessException;
import com.skada.common.util.DistributedLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 租户API触发排行榜手动滚动服务
 */
@Service
public class LeaderboardRollService {

    private final TenantAuthService tenantAuthService;
    private final LeaderboardMapper leaderboardMapper;
    private final LeaderboardInstanceMapper instanceMapper;
    private final DistributedLock distributedLock;

    public LeaderboardRollService(TenantAuthService tenantAuthService,
                                  LeaderboardMapper leaderboardMapper,
                                  LeaderboardInstanceMapper instanceMapper,
                                  DistributedLock distributedLock) {
        this.tenantAuthService = tenantAuthService;
        this.leaderboardMapper = leaderboardMapper;
        this.instanceMapper = instanceMapper;
        this.distributedLock = distributedLock;
    }

    @Transactional
    public RollLeaderboardResponse roll(RollLeaderboardRequest request) {
        // 1. 租户鉴权
        Tenant tenant = tenantAuthService.authenticate(request.getTenantId(), request.getSecretKey());
        if (tenant == null) {
            throw new BusinessException("租户校验失败");
        }

        // 2. 查询排行榜
        Leaderboard lb = leaderboardMapper.findByPlanId(request.getPlanId());

        // 3. 存在性+归属校验（合并，防止枚举遍历）
        if (lb == null || !request.getTenantId().equals(lb.getTenantId())) {
            throw new BusinessException("排行榜不存在或无权操作");
        }

        // 4. 状态校验
        if (!"active".equals(lb.getStatus())) {
            throw new BusinessException("排行榜已终止，无法滚动");
        }

        // 5. 实例ID校验
        LeaderboardInstance activeInstance = instanceMapper.findActiveByLeaderboardId(lb.getId());
        validateInstanceId(request.getInstanceId(), activeInstance);

        // 6. 分布式锁
        String lockValue = UUID.randomUUID().toString();
        String lockName = "roll:leaderboard:" + lb.getId();
        if (!distributedLock.tryLock(lockName, lockValue, 10, TimeUnit.SECONDS)) {
            throw new BusinessException(409, "排行榜正在滚动中，请稍后重试");
        }

        try {
            // 7. 双重检查实例ID
            activeInstance = instanceMapper.findActiveByLeaderboardId(lb.getId());
            validateInstanceId(request.getInstanceId(), activeInstance);

            // 8. 关闭旧实例
            instanceMapper.closeInstance(activeInstance.getId(), System.currentTimeMillis());

            // 9. 创建新实例
            int maxSeq = instanceMapper.getMaxInstanceSeq(lb.getId());
            LeaderboardInstance newInstance = new LeaderboardInstance();
            newInstance.setInstanceId("li_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
            newInstance.setLeaderboardId(lb.getId());
            newInstance.setInstanceSeq(maxSeq + 1);
            newInstance.setStartTime(System.currentTimeMillis());
            newInstance.setStatus("active");
            newInstance.setCreateBy(request.getTenantId());
            newInstance.setUpdateBy(request.getTenantId());
            instanceMapper.insert(newInstance);

            // 10. 更新排行榜当前实例
            lb.setCurrentInstanceId(newInstance.getId());
            leaderboardMapper.update(lb);

            return new RollLeaderboardResponse(lb.getPlanId(), newInstance.getInstanceId(), newInstance.getInstanceSeq());
        } finally {
            distributedLock.unlock(lockName, lockValue);
        }
    }

    private void validateInstanceId(String requestInstanceId, LeaderboardInstance activeInstance) {
        boolean hasActive = activeInstance != null;
        boolean passedEmpty = requestInstanceId == null || requestInstanceId.isEmpty();

        if (hasActive && passedEmpty) {
            throw new BusinessException("请提供当前活跃实例ID");
        }
        if (!hasActive && !passedEmpty) {
            throw new BusinessException("当前无活跃实例");
        }
        if (hasActive && !activeInstance.getInstanceId().equals(requestInstanceId)) {
            throw new BusinessException(409, "排行榜实例已变更，请重新查询后重试");
        }
    }
}
