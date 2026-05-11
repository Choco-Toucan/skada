package com.skada.mng.scheduler;

import com.skada.common.util.DistributedLock;
import com.skada.mng.mapper.LeaderboardCycleMapper;
import com.skada.mng.mapper.LeaderboardMapper;
import com.skada.mng.model.Leaderboard;
import com.skada.mng.model.LeaderboardCycle;
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
    private final LeaderboardCycleMapper cycleMapper;
    private final DistributedLock distributedLock;

    public LeaderboardRollScheduler(LeaderboardMapper leaderboardMapper,
                                    LeaderboardCycleMapper cycleMapper,
                                    DistributedLock distributedLock) {
        this.leaderboardMapper = leaderboardMapper;
        this.cycleMapper = cycleMapper;
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

                LeaderboardCycle activeCycle = cycleMapper.findActiveByLeaderboardId(lb.getId());
                if (activeCycle == null) {
                    continue;
                }

                long intervalMs = toMilliseconds(lb.getRollIntervalValue(), lb.getRollIntervalUnit());
                if (intervalMs <= 0) {
                    continue;
                }

                long cycleEndTime = activeCycle.getCycleStartTime() + intervalMs;
                if (now >= cycleEndTime) {
                    log.info("触发周期性滚动: leaderboardId={}, cycleSeq={}",
                            lb.getId(), activeCycle.getCycleSeq());
                    rollLeaderboard(lb.getId(), "scheduler");
                }
            }
        } finally {
            distributedLock.unlock("scheduler:periodic-roll", lockValue);
        }
    }

    @Transactional
    public void stopLeaderboard(Long leaderboardId) {
        // 关闭当前活跃周期
        LeaderboardCycle activeCycle = cycleMapper.findActiveByLeaderboardId(leaderboardId);
        if (activeCycle != null) {
            cycleMapper.closeCycle(activeCycle.getId(), System.currentTimeMillis());
        }

        // 标记排行榜已终止
        Leaderboard lb = leaderboardMapper.findById(leaderboardId);
        if (lb != null) {
            lb.setStatus("stopped");
            lb.setUpdateBy("scheduler");
            leaderboardMapper.update(lb);
        }
    }

    @Transactional
    public void rollLeaderboard(Long leaderboardId, String adminId) {
        LeaderboardCycle activeCycle = cycleMapper.findActiveByLeaderboardId(leaderboardId);
        if (activeCycle != null) {
            cycleMapper.closeCycle(activeCycle.getId(), System.currentTimeMillis());
        }

        int maxSeq = cycleMapper.getMaxCycleSeq(leaderboardId);
        LeaderboardCycle newCycle = new LeaderboardCycle();
        newCycle.setLeaderboardId(leaderboardId);
        newCycle.setCycleSeq(maxSeq + 1);
        newCycle.setCycleStartTime(System.currentTimeMillis());
        newCycle.setStatus("active");
        newCycle.setCreateBy(adminId);
        newCycle.setUpdateBy(adminId);
        cycleMapper.insert(newCycle);

        Leaderboard lb = leaderboardMapper.findById(leaderboardId);
        if (lb != null) {
            lb.setCurrentCycleId(newCycle.getId());
            leaderboardMapper.update(lb);
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
