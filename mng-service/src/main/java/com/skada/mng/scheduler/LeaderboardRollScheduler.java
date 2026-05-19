package com.skada.mng.scheduler;

import com.skada.common.util.DistributedLock;
import com.skada.mng.mapper.LeaderboardInstanceMapper;
import com.skada.mng.mapper.LeaderboardMapper;
import com.skada.mng.model.Leaderboard;
import com.skada.mng.model.LeaderboardInstance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 排行榜滚动调度器
 * <p>
 * 负责周期性滚动策略的自动触发。
 * 使用分布式锁确保多节点下只有一个实例执行滚动。
 * </p>
 */
@Component
public class LeaderboardRollScheduler {

    private static final Logger log = LogManager.getLogger(LeaderboardRollScheduler.class);

    private final LeaderboardMapper leaderboardMapper;
    private final LeaderboardInstanceMapper instanceMapper;
    private final DistributedLock distributedLock;

    public LeaderboardRollScheduler(LeaderboardMapper leaderboardMapper,
                                    LeaderboardInstanceMapper instanceMapper,
                                    DistributedLock distributedLock) {
        this.leaderboardMapper = leaderboardMapper;
        this.instanceMapper = instanceMapper;
        this.distributedLock = distributedLock;
    }

    /**
     * 每分钟检查一次是否需要周期性滚动
     */
    @Scheduled(fixedRate = 60_000)
    public void checkPeriodicRoll() {
        String lockValue = UUID.randomUUID().toString();
        // 分布式锁，防止多节点重复执行
        if (!distributedLock.tryLock("scheduler:periodic-roll", lockValue, 30, TimeUnit.SECONDS)) {
            return;
        }

        try {
            List<Leaderboard> allLeaderboards = leaderboardMapper.findAll();
            long now = System.currentTimeMillis();

            for (Leaderboard lb : allLeaderboards) {
                // 仅处理活跃排行榜
                if (!"active".equals(lb.getStatus())) {
                    continue;
                }

                // 检查结束时间是否已到，自动终止
                if (lb.getEndTime() != null && now >= lb.getEndTime()) {
                    log.info("排行榜到期自动终止: leaderboardId={}", lb.getId());
                    stopLeaderboard(lb.getId());
                    continue;
                }

                // 仅处理周期性滚动
                if (!"periodic".equals(lb.getRollStrategy())) {
                    continue;
                }

                LeaderboardInstance activeInstance = instanceMapper.findActiveByLeaderboardId(lb.getId());
                if (activeInstance == null) {
                    continue;
                }

                long intervalMs = toMilliseconds(lb.getRollIntervalValue(), lb.getRollIntervalUnit());
                if (intervalMs <= 0) {
                    continue;
                }

                long instanceEndTime = activeInstance.getStartTime() + intervalMs;
                if (now >= instanceEndTime) {
                    log.info("触发周期性滚动: leaderboardId={}, instanceSeq={}",
                            lb.getId(), activeInstance.getInstanceSeq());
                    rollLeaderboard(lb.getId(), "scheduler");
                }
            }
        } finally {
            distributedLock.unlock("scheduler:periodic-roll", lockValue);
        }
    }

    @Transactional
    public void stopLeaderboard(Long leaderboardId) {
        String lockValue = UUID.randomUUID().toString();
        String lockName = "roll:leaderboard:" + leaderboardId;
        if (!distributedLock.tryLock(lockName, lockValue, 10, TimeUnit.SECONDS)) {
            log.warn("排行榜操作进行中，跳过自动终止: leaderboardId={}", leaderboardId);
            return;
        }

        try {
            LeaderboardInstance activeInstance = instanceMapper.findActiveByLeaderboardId(leaderboardId);
            if (activeInstance != null) {
                instanceMapper.closeInstance(activeInstance.getId(), System.currentTimeMillis());
            }

            Leaderboard lb = leaderboardMapper.findById(leaderboardId);
            if (lb != null) {
                lb.setStatus("stopped");
                lb.setUpdateBy("scheduler");
                leaderboardMapper.update(lb);
            }
        } finally {
            distributedLock.unlock(lockName, lockValue);
        }
    }

    @Transactional
    public void rollLeaderboard(Long leaderboardId, String adminId) {
        String lockValue = UUID.randomUUID().toString();
        String lockName = "roll:leaderboard:" + leaderboardId;
        if (!distributedLock.tryLock(lockName, lockValue, 10, TimeUnit.SECONDS)) {
            log.warn("排行榜正在滚动中，跳过自动滚动: leaderboardId={}", leaderboardId);
            return;
        }

        try {
            LeaderboardInstance activeInstance = instanceMapper.findActiveByLeaderboardId(leaderboardId);
            if (activeInstance == null) {
                return;
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

            Leaderboard lb = leaderboardMapper.findById(leaderboardId);
            if (lb != null) {
                lb.setCurrentInstanceId(newInstance.getId());
                leaderboardMapper.update(lb);
            }
        } finally {
            distributedLock.unlock(lockName, lockValue);
        }
    }

    private long toMilliseconds(Integer value, String unit) {
        if (value == null || unit == null) return 0;
        return switch (unit) {
            case "minute" -> value * 60_000L;
            case "hour" -> value * 3_600_000L;
            case "day" -> value * 86_400_000L;
            default -> 0;
        };
    }
}
