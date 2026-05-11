package com.skada.api.service;

import com.skada.api.mapper.LeaderboardCycleMapper;
import com.skada.api.mapper.LeaderboardMapper;
import com.skada.api.mapper.ScoreRecordMapper;
import com.skada.api.model.Leaderboard;
import com.skada.api.model.LeaderboardCycle;
import com.skada.api.model.ScoreRecord;
import com.skada.api.model.Tenant;
import com.skada.common.exception.BusinessException;
import com.skada.common.util.DistributedLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 分数上报服务
 * 写入Redis ZSET实现实时排名，同时持久化到MySQL
 */
@Service
public class ScoreService {

    private static final Logger log = LogManager.getLogger(ScoreService.class);

    private static final String RANKING_KEY_PREFIX = "skada:leaderboard:%d:cycle:%d";
    private static final String USER_COUNT_KEY_PREFIX = "skada:leaderboard:%d:cycle:%d:users";
    /** 批量上报单次最大条数，防止超过MySQL max_allowed_packet */
    private static final int BATCH_MAX_SIZE = 1000;

    private final TenantAuthService tenantAuthService;
    private final LeaderboardMapper leaderboardMapper;
    private final LeaderboardCycleMapper cycleMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final StringRedisTemplate redisTemplate;
    private final DistributedLock distributedLock;

    public ScoreService(TenantAuthService tenantAuthService,
                        LeaderboardMapper leaderboardMapper,
                        LeaderboardCycleMapper cycleMapper,
                        ScoreRecordMapper scoreRecordMapper,
                        StringRedisTemplate redisTemplate,
                        DistributedLock distributedLock) {
        this.tenantAuthService = tenantAuthService;
        this.leaderboardMapper = leaderboardMapper;
        this.cycleMapper = cycleMapper;
        this.scoreRecordMapper = scoreRecordMapper;
        this.redisTemplate = redisTemplate;
        this.distributedLock = distributedLock;
    }

    /**
     * 单条分数上报
     */
    @Transactional
    public ScoreRecord submit(String tenantId, String secretKey,
                              Long leaderboardId, String userId,
                              BigDecimal score, String payload) {
        // 鉴权
        Tenant tenant = tenantAuthService.authenticate(tenantId, secretKey);
        if (tenant == null) {
            throw new BusinessException(401, "租户鉴权失败");
        }

        // 获取排行榜和活跃周期
        Leaderboard lb = leaderboardMapper.findById(leaderboardId);
        if (lb == null || !"active".equals(lb.getStatus())) {
            throw new BusinessException("排行榜不存在或已终止");
        }
        if (!lb.getTenantId().equals(tenantId)) {
            throw new BusinessException("排行榜不属于该租户");
        }
        // 排行榜尚未开始
        if (lb.getStartTime() > System.currentTimeMillis()) {
            throw new BusinessException("排行榜尚未开始");
        }

        LeaderboardCycle cycle = cycleMapper.findActiveByLeaderboardId(leaderboardId);
        if (cycle == null) {
            throw new BusinessException("当前没有活跃的排行榜周期");
        }

        // 检查是否允许重复上报
        if (lb.getAllowDuplicateReport() == 0) {
            ScoreRecord existing = scoreRecordMapper.findByUserAndCycle(
                    leaderboardId, cycle.getId(), userId);
            if (existing != null) {
                throw new BusinessException("该用户已上报过分数，不允许重复上报");
            }
        }

        // 写入Redis ZSET
        String rankingKey = String.format(RANKING_KEY_PREFIX, leaderboardId, cycle.getId());
        redisTemplate.opsForZSet().add(rankingKey, userId, score.doubleValue());

        // 持久化到MySQL
        ScoreRecord record = new ScoreRecord();
        record.setTenantId(tenantId);
        record.setLeaderboardId(leaderboardId);
        record.setCycleId(cycle.getId());
        record.setUserId(userId);
        record.setScore(score);
        record.setPayload(payload);
        scoreRecordMapper.insert(record);

        // 记录去重用户数（HyperLogLog），用于用户数滚动策略
        String userCountKey = String.format(USER_COUNT_KEY_PREFIX, leaderboardId, cycle.getId());
        redisTemplate.opsForHyperLogLog().add(userCountKey, userId);

        // 检查是否需要按用户数滚动
        checkUserCountRoll(lb, cycle);

        return record;
    }

    /**
     * 批量分数上报（同一排行榜）
     */
    @Transactional
    public void batchSubmit(String tenantId, String secretKey,
                            Long leaderboardId,
                            List<ScoreSubmitItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("上报数据不能为空");
        }
        if (items.size() > BATCH_MAX_SIZE) {
            throw new BusinessException("单次批量上报不能超过" + BATCH_MAX_SIZE + "条");
        }

        // 鉴权
        Tenant tenant = tenantAuthService.authenticate(tenantId, secretKey);
        if (tenant == null) {
            throw new BusinessException(401, "租户鉴权失败");
        }

        // 获取排行榜和活跃周期
        Leaderboard lb = leaderboardMapper.findById(leaderboardId);
        if (lb == null || !"active".equals(lb.getStatus())) {
            throw new BusinessException("排行榜不存在或已终止");
        }
        if (!lb.getTenantId().equals(tenantId)) {
            throw new BusinessException("排行榜不属于该租户");
        }
        if (lb.getStartTime() > System.currentTimeMillis()) {
            throw new BusinessException("排行榜尚未开始");
        }

        LeaderboardCycle cycle = cycleMapper.findActiveByLeaderboardId(leaderboardId);
        if (cycle == null) {
            throw new BusinessException("当前没有活跃的排行榜周期");
        }

        // 检查是否允许重复上报
        if (lb.getAllowDuplicateReport() == 0) {
            for (ScoreSubmitItem item : items) {
                ScoreRecord existing = scoreRecordMapper.findByUserAndCycle(
                        leaderboardId, cycle.getId(), item.getUserId());
                if (existing != null) {
                    throw new BusinessException("用户 " + item.getUserId() + " 已上报过分数，不允许重复上报");
                }
            }
        }

        String rankingKey = String.format(RANKING_KEY_PREFIX, leaderboardId, cycle.getId());
        String userCountKey = String.format(USER_COUNT_KEY_PREFIX, leaderboardId, cycle.getId());

        // 逐条处理
        for (ScoreSubmitItem item : items) {
            redisTemplate.opsForZSet().add(rankingKey, item.getUserId(),
                    item.getScore().doubleValue());
            redisTemplate.opsForHyperLogLog().add(userCountKey, item.getUserId());
        }

        // 批量写入MySQL
        List<ScoreRecord> records = items.stream().map(item -> {
            ScoreRecord r = new ScoreRecord();
            r.setTenantId(tenantId);
            r.setLeaderboardId(leaderboardId);
            r.setCycleId(cycle.getId());
            r.setUserId(item.getUserId());
            r.setScore(item.getScore());
            r.setPayload(item.getPayload());
            return r;
        }).toList();
        scoreRecordMapper.insertBatch(records);

        // 检查是否需要按用户数滚动
        checkUserCountRoll(lb, cycle);
    }

    /**
     * 检查是否需要按用户数触发滚动
     */
    private void checkUserCountRoll(Leaderboard lb, LeaderboardCycle cycle) {
        if (!"user_count".equals(lb.getRollStrategy())
                || lb.getRollUserCount() == null
                || lb.getRollUserCount() <= 0) {
            return;
        }

        // 使用Redis HyperLogLog统计近似用户数，高效且节省内存
        String userCountKey = String.format(USER_COUNT_KEY_PREFIX,
                lb.getId(), cycle.getId());
        // HyperLogLog计数可能已有数据，用PFCOUNT获取
        // 写一条数据时，用PFCOUNT可能存在误差，取MAX(PFCOUNT, DB COUNT)做参考
        // 但为了确保触发，先用PFCOUNT大概判断，再用DB精确值兜底
        Long approxCount = redisTemplate.opsForHyperLogLog().size(userCountKey);
        if (approxCount == null) return;

        // 如果近似值已经达到阈值，精确检查DB
        if (approxCount >= lb.getRollUserCount()) {
            int exactCount = scoreRecordMapper.countDistinctUsersByCycle(
                    lb.getId(), cycle.getId());
            if (exactCount >= lb.getRollUserCount()) {
                // 使用分布式锁触发滚动
                String lockValue = UUID.randomUUID().toString();
                String lockName = "roll:user-count:" + lb.getId();
                if (distributedLock.tryLock(lockName, lockValue, 10, TimeUnit.SECONDS)) {
                    try {
                        // 锁内再检查一次，防止并发
                        LeaderboardCycle stillActive = cycleMapper
                                .findActiveByLeaderboardId(lb.getId());
                        if (stillActive != null
                                && stillActive.getId().equals(cycle.getId())) {
                            log.info("触发用户数滚动: leaderboardId={}, cycleSeq={}, userCount={}",
                                    lb.getId(), cycle.getCycleSeq(), exactCount);
                            doRoll(lb.getId());
                        }
                    } finally {
                        distributedLock.unlock(lockName, lockValue);
                    }
                }
            }
        }
    }

    /**
     * 执行滚动：关闭当前周期，创建新周期
     */
    @Transactional
    public void doRoll(Long leaderboardId) {
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
        newCycle.setCreateBy("system");
        newCycle.setUpdateBy("system");
        cycleMapper.insert(newCycle);

        Leaderboard lb = leaderboardMapper.findById(leaderboardId);
        if (lb != null) {
            lb.setCurrentCycleId(newCycle.getId());
            leaderboardMapper.update(lb);
        }

        log.info("排行榜滚动完成: leaderboardId={}, newCycleId={}, cycleSeq={}",
                leaderboardId, newCycle.getId(), maxSeq + 1);
    }

    /**
     * 批量上报中的单条数据
     */
    public static class ScoreSubmitItem {
        private String userId;
        private BigDecimal score;
        private String payload;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public BigDecimal getScore() { return score; }
        public void setScore(BigDecimal score) { this.score = score; }
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
    }
}
