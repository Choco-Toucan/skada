package com.skada.api.service;

import com.skada.api.mapper.LeaderboardInstanceMapper;
import com.skada.api.mapper.LeaderboardMapper;
import com.skada.api.mapper.LeaderboardMetricMapper;
import com.skada.api.mapper.ScoreRecordMapper;
import com.skada.api.model.Leaderboard;
import com.skada.api.model.LeaderboardInstance;
import com.skada.api.model.LeaderboardMetric;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 分数上报服务
 * 按指标维度写入Redis ZSET实现实时排名，同时持久化到MySQL
 */
@Service
public class ScoreService {

    private static final Logger log = LogManager.getLogger(ScoreService.class);

    private static final String RANKING_KEY_PREFIX = "skada:metric:%d:%d:%d";
    private static final String USER_COUNT_KEY_PREFIX = "skada:leaderboard:%d:instance:%d:users";
    private static final int BATCH_MAX_SIZE = 1000;

    private final TenantAuthService tenantAuthService;
    private final LeaderboardMapper leaderboardMapper;
    private final LeaderboardInstanceMapper instanceMapper;
    private final LeaderboardMetricMapper leaderboardMetricMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final StringRedisTemplate redisTemplate;
    private final DistributedLock distributedLock;

    public ScoreService(TenantAuthService tenantAuthService,
                        LeaderboardMapper leaderboardMapper,
                        LeaderboardInstanceMapper instanceMapper,
                        LeaderboardMetricMapper leaderboardMetricMapper,
                        ScoreRecordMapper scoreRecordMapper,
                        StringRedisTemplate redisTemplate,
                        DistributedLock distributedLock) {
        this.tenantAuthService = tenantAuthService;
        this.leaderboardMapper = leaderboardMapper;
        this.instanceMapper = instanceMapper;
        this.leaderboardMetricMapper = leaderboardMetricMapper;
        this.scoreRecordMapper = scoreRecordMapper;
        this.redisTemplate = redisTemplate;
        this.distributedLock = distributedLock;
    }

    /**
     * 单条分数上报（含多指标值）
     */
    @Transactional
    public void submit(String tenantId, String secretKey,
                       Long leaderboardId, String userId,
                       List<MetricValue> metrics, String payload) {
        Tenant tenant = tenantAuthService.authenticate(tenantId, secretKey);
        if (tenant == null) {
            throw new BusinessException(401, "租户鉴权失败");
        }

        Leaderboard lb = validateAndGetLeaderboard(leaderboardId, tenantId);
        LeaderboardInstance instance = validateAndGetActiveInstance(leaderboardId);
        List<LeaderboardMetric> lbMetrics = validateMetrics(leaderboardId, metrics);

        // 检查是否允许重复上报
        if (lb.getAllowDuplicateReport() == 0) {
            ScoreRecord existing = scoreRecordMapper.findByUserAndInstance(
                    leaderboardId, instance.getId(), userId);
            if (existing != null) {
                throw new BusinessException("该用户已上报过分数，不允许重复上报");
            }
        }

        // 写入Redis和MySQL
        String userCountKey = String.format(USER_COUNT_KEY_PREFIX, leaderboardId, instance.getId());
        for (MetricValue mv : metrics) {
            String rankingKey = String.format(RANKING_KEY_PREFIX, leaderboardId, instance.getId(), mv.getMetricId());
            redisTemplate.opsForZSet().add(rankingKey, userId, mv.getValue().doubleValue());
        }
        redisTemplate.opsForHyperLogLog().add(userCountKey, userId);

        // 持久化
        List<ScoreRecord> records = new ArrayList<>();
        for (MetricValue mv : metrics) {
            ScoreRecord r = new ScoreRecord();
            r.setTenantId(tenantId);
            r.setLeaderboardId(leaderboardId);
            r.setInstanceId(instance.getId());
            r.setMetricId(mv.getMetricId());
            r.setUserId(userId);
            r.setScore(mv.getValue());
            r.setPayload(payload);
            records.add(r);
        }
        scoreRecordMapper.insertBatch(records);

        checkUserCountRoll(lb, instance);
    }

    /**
     * 批量分数上报（同一排行榜计划）
     */
    @Transactional
    public void batchSubmit(String tenantId, String secretKey,
                            Long leaderboardId, List<BatchSubmitItem> items) {
        if (items.size() > BATCH_MAX_SIZE) {
            throw new BusinessException("单次批量上报不能超过" + BATCH_MAX_SIZE + "条");
        }

        Tenant tenant = tenantAuthService.authenticate(tenantId, secretKey);
        if (tenant == null) {
            throw new BusinessException(401, "租户鉴权失败");
        }

        Leaderboard lb = validateAndGetLeaderboard(leaderboardId, tenantId);
        LeaderboardInstance instance = validateAndGetActiveInstance(leaderboardId);

        // 收集所有metricId用于校验
        List<Long> allMetricIds = new ArrayList<>();
        for (var item : items) {
            for (var mv : item.getMetrics()) {
                if (!allMetricIds.contains(mv.getMetricId())) {
                    allMetricIds.add(mv.getMetricId());
                }
            }
        }
        for (Long metricId : allMetricIds) {
            boolean found = false;
            for (LeaderboardMetric lm : leaderboardMetricMapper.findByLeaderboardId(leaderboardId)) {
                if (lm.getMetricId().equals(metricId)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new BusinessException("指标 " + metricId + " 不属于该排行榜计划");
            }
        }

        if (lb.getAllowDuplicateReport() == 0) {
            for (var item : items) {
                ScoreRecord existing = scoreRecordMapper.findByUserAndInstance(
                        leaderboardId, instance.getId(), item.getUserId());
                if (existing != null) {
                    throw new BusinessException("用户 " + item.getUserId() + " 已上报过分数，不允许重复上报");
                }
            }
        }

        String userCountKey = String.format(USER_COUNT_KEY_PREFIX, leaderboardId, instance.getId());
        List<ScoreRecord> allRecords = new ArrayList<>();
        for (var item : items) {
            for (var mv : item.getMetrics()) {
                String rankingKey = String.format(RANKING_KEY_PREFIX, leaderboardId, instance.getId(), mv.getMetricId());
                redisTemplate.opsForZSet().add(rankingKey, item.getUserId(), mv.getValue().doubleValue());

                ScoreRecord r = new ScoreRecord();
                r.setTenantId(tenantId);
                r.setLeaderboardId(leaderboardId);
                r.setInstanceId(instance.getId());
                r.setMetricId(mv.getMetricId());
                r.setUserId(item.getUserId());
                r.setScore(mv.getValue());
                r.setPayload(item.getPayload());
                allRecords.add(r);
            }
            redisTemplate.opsForHyperLogLog().add(userCountKey, item.getUserId());
        }

        scoreRecordMapper.insertBatch(allRecords);
        checkUserCountRoll(lb, instance);
    }

    private Leaderboard validateAndGetLeaderboard(Long leaderboardId, String tenantId) {
        Leaderboard lb = leaderboardMapper.findById(leaderboardId);
        if (lb == null || !"active".equals(lb.getStatus())) {
            throw new BusinessException("排行榜计划不存在或已终止");
        }
        if (!lb.getTenantId().equals(tenantId)) {
            throw new BusinessException("排行榜计划不属于该租户");
        }
        if (lb.getStartTime() > System.currentTimeMillis()) {
            throw new BusinessException("排行榜计划尚未开始");
        }
        return lb;
    }

    private LeaderboardInstance validateAndGetActiveInstance(Long leaderboardId) {
        LeaderboardInstance instance = instanceMapper.findActiveByLeaderboardId(leaderboardId);
        if (instance == null) {
            throw new BusinessException("当前没有活跃的排行榜实例");
        }
        return instance;
    }

    private List<LeaderboardMetric> validateMetrics(Long leaderboardId, List<MetricValue> metrics) {
        List<LeaderboardMetric> lbMetrics = leaderboardMetricMapper.findByLeaderboardId(leaderboardId);
        for (MetricValue mv : metrics) {
            boolean found = false;
            for (LeaderboardMetric lm : lbMetrics) {
                if (lm.getMetricId().equals(mv.getMetricId())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new BusinessException("指标 " + mv.getMetricId() + " 不属于该排行榜计划");
            }
        }
        return lbMetrics;
    }

    private void checkUserCountRoll(Leaderboard lb, LeaderboardInstance instance) {
        if (!"user_count".equals(lb.getRollStrategy())
                || lb.getRollUserCount() == null
                || lb.getRollUserCount() <= 0) {
            return;
        }

        String userCountKey = String.format(USER_COUNT_KEY_PREFIX, lb.getId(), instance.getId());
        Long approxCount = redisTemplate.opsForHyperLogLog().size(userCountKey);
        if (approxCount == null) return;

        if (approxCount >= lb.getRollUserCount()) {
            int exactCount = scoreRecordMapper.countDistinctUsersByInstance(lb.getId(), instance.getId());
            if (exactCount >= lb.getRollUserCount()) {
                String lockValue = UUID.randomUUID().toString();
                String lockName = "roll:user-count:" + lb.getId();
                if (distributedLock.tryLock(lockName, lockValue, 10, TimeUnit.SECONDS)) {
                    try {
                        LeaderboardInstance stillActive = instanceMapper.findActiveByLeaderboardId(lb.getId());
                        if (stillActive != null && stillActive.getId().equals(instance.getId())) {
                            log.info("触发用户数滚动: leaderboardId={}, instanceSeq={}, userCount={}",
                                    lb.getId(), instance.getInstanceSeq(), exactCount);
                            doRoll(lb.getId());
                        }
                    } finally {
                        distributedLock.unlock(lockName, lockValue);
                    }
                }
            }
        }
    }

    @Transactional
    public void doRoll(Long leaderboardId) {
        LeaderboardInstance activeInstance = instanceMapper.findActiveByLeaderboardId(leaderboardId);
        if (activeInstance != null) {
            instanceMapper.closeInstance(activeInstance.getId(), System.currentTimeMillis());
        }

        int maxSeq = instanceMapper.getMaxInstanceSeq(leaderboardId);
        LeaderboardInstance newInstance = new LeaderboardInstance();
        newInstance.setLeaderboardId(leaderboardId);
        newInstance.setInstanceSeq(maxSeq + 1);
        newInstance.setStartTime(System.currentTimeMillis());
        newInstance.setStatus("active");
        newInstance.setCreateBy("system");
        newInstance.setUpdateBy("system");
        instanceMapper.insert(newInstance);

        Leaderboard lb = leaderboardMapper.findById(leaderboardId);
        if (lb != null) {
            lb.setCurrentInstanceId(newInstance.getId());
            leaderboardMapper.update(lb);
        }

        log.info("排行榜滚动完成: leaderboardId={}, newInstanceId={}, instanceSeq={}",
                leaderboardId, newInstance.getId(), maxSeq + 1);
    }

    /**
     * 单个指标值
     */
    public static class MetricValue {
        private Long metricId;
        private BigDecimal value;

        public Long getMetricId() { return metricId; }
        public void setMetricId(Long metricId) { this.metricId = metricId; }
        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal value) { this.value = value; }
    }

    /**
     * 批量上报中的单条数据
     */
    public static class BatchSubmitItem {
        private String userId;
        private List<MetricValue> metrics;
        private String payload;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public List<MetricValue> getMetrics() { return metrics; }
        public void setMetrics(List<MetricValue> metrics) { this.metrics = metrics; }
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
    }
}
