package com.skada.api.service;

import com.skada.api.mapper.LeaderboardInstanceMapper;
import com.skada.api.mapper.LeaderboardMapper;
import com.skada.api.mapper.LeaderboardMetricMapper;
import com.skada.api.mapper.MetricMapper;
import com.skada.api.mapper.ScoreRecordMapper;
import com.skada.api.model.*;
import com.skada.common.exception.BusinessException;
import com.skada.common.util.DistributedLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 分数上报服务
 * 按指标维度写入Redis ZSET实现实时排名，同时持久化到MySQL
 * 服务端根据上报的指标集合自动关联对应的排行榜计划
 */
@Service
public class ScoreService {

    private static final Logger log = LogManager.getLogger(ScoreService.class);

    private static final String RANKING_KEY_PREFIX = "skada:metric:%d:%d:%d";
    private static final String USER_COUNT_KEY_PREFIX = "skada:leaderboard:%d:instance:%d:users";
    private static final int BATCH_MAX_SIZE = 1000;

    private final LeaderboardMapper leaderboardMapper;
    private final LeaderboardInstanceMapper instanceMapper;
    private final LeaderboardMetricMapper leaderboardMetricMapper;
    private final MetricMapper metricMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final StringRedisTemplate redisTemplate;
    private final DistributedLock distributedLock;

    public ScoreService(LeaderboardMapper leaderboardMapper,
                        LeaderboardInstanceMapper instanceMapper,
                        LeaderboardMetricMapper leaderboardMetricMapper,
                        MetricMapper metricMapper,
                        ScoreRecordMapper scoreRecordMapper,
                        StringRedisTemplate redisTemplate,
                        DistributedLock distributedLock) {
        this.leaderboardMapper = leaderboardMapper;
        this.instanceMapper = instanceMapper;
        this.leaderboardMetricMapper = leaderboardMetricMapper;
        this.metricMapper = metricMapper;
        this.scoreRecordMapper = scoreRecordMapper;
        this.redisTemplate = redisTemplate;
        this.distributedLock = distributedLock;
    }

    /**
     * 单条分数上报（含多指标值）
     * 服务端根据上报的指标集合自动解析对应的所有排行榜计划并分别更新
     * <p>租户身份由 SaasAuthFilter 校验后注入，此处直接使用。</p>
     */
    @Transactional
    public void submit(String tenantId, String userId, List<MetricValue> metrics) {
        // 解析外部metricId为内部Long id
        Map<String, Long> metricIdMap = resolveMetricIds(metrics);
        List<Long> internalMetricIds = new ArrayList<>(metricIdMap.values());

        // 根据指标集合解析所有匹配的排行榜计划
        List<Leaderboard> plans = resolveLeaderboardPlansByMetricIds(tenantId, internalMetricIds);

        for (Leaderboard lb : plans) {
            LeaderboardInstance instance = validateAndGetActiveInstance(lb.getId());

            // 校验指标属于该计划
            validateMetricsAgainstPlan(lb.getId(), metrics, metricIdMap);

            // 检查是否允许重复上报（增量模式不限制）
            if (lb.getAllowDuplicateReport() == 0 && !isAllIncrement(metrics)) {
                ScoreRecord existing = scoreRecordMapper.findByUserAndInstance(
                        lb.getId(), instance.getId(), userId);
                if (existing != null) {
                    throw new BusinessException("该用户已上报过分数，不允许重复上报");
                }
            }

            writeMetrics(tenantId, lb.getId(), instance.getId(), userId, metrics, metricIdMap);

            String userCountKey = String.format(USER_COUNT_KEY_PREFIX, lb.getId(), instance.getId());
            redisTemplate.opsForHyperLogLog().add(userCountKey, userId);

            checkUserCountRoll(lb, instance);
        }
    }

    /**
     * 批量分数上报
     * 根据指标集合解析所有匹配的排行榜计划并分别更新
     * <p>租户身份由 SaasAuthFilter 校验后注入，此处直接使用。</p>
     */
    @Transactional
    public void batchSubmit(String tenantId, List<BatchSubmitItem> items) {
        if (items.size() > BATCH_MAX_SIZE) {
            throw new BusinessException("单次批量上报不能超过" + BATCH_MAX_SIZE + "条");
        }

        // 解析外部metricId为内部Long id
        Map<String, Long> metricIdMap = resolveMetricIds(items.get(0).getMetrics());
        List<Long> internalMetricIds = new ArrayList<>(metricIdMap.values());

        // 所有条目的指标集合必须一致
        String firstMetricIds = items.get(0).getMetrics().stream()
                .map(MetricValue::getMetricId).sorted().collect(Collectors.joining(","));
        for (var item : items) {
            String itemMetricIds = item.getMetrics().stream()
                    .map(MetricValue::getMetricId).sorted().collect(Collectors.joining(","));
            if (!firstMetricIds.equals(itemMetricIds)) {
                throw new BusinessException("批量上报中每条数据的指标集合必须一致");
            }
        }

        List<Leaderboard> plans = resolveLeaderboardPlansByMetricIds(tenantId, internalMetricIds);

        for (Leaderboard lb : plans) {
            LeaderboardInstance instance = validateAndGetActiveInstance(lb.getId());

            if (lb.getAllowDuplicateReport() == 0) {
                for (var item : items) {
                    if (isAllIncrement(item.getMetrics())) {
                        continue;
                    }
                    ScoreRecord existing = scoreRecordMapper.findByUserAndInstance(
                            lb.getId(), instance.getId(), item.getUserId());
                    if (existing != null) {
                        throw new BusinessException("用户 " + item.getUserId() + " 已上报过分数，不允许重复上报");
                    }
                }
            }

            String userCountKey = String.format(USER_COUNT_KEY_PREFIX, lb.getId(), instance.getId());
            for (var item : items) {
                writeMetrics(tenantId, lb.getId(), instance.getId(), item.getUserId(), item.getMetrics(), metricIdMap);
                redisTemplate.opsForHyperLogLog().add(userCountKey, item.getUserId());
            }

            checkUserCountRoll(lb, instance);
        }
    }

    /**
     * 根据上报的指标集合解析对应的排行榜计划ID
     * 一个指标集合必须唯一对应一个排行榜计划，否则报错
     */
    private Map<String, Long> resolveMetricIds(List<MetricValue> metrics) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (MetricValue mv : metrics) {
            Metric m = metricMapper.findByMetricId(mv.getMetricId());
            if (m == null) {
                throw new BusinessException("指标不存在: " + mv.getMetricId());
            }
            map.put(mv.getMetricId(), m.getId());
        }
        return map;
    }

    /**
     * 根据上报的指标集合解析所有匹配的排行榜计划
     * 上报的指标集合必须包含某计划需要的全部指标（但计划可包含额外指标）
     * 一份上报数据可同时更新多个排行榜计划（如日榜+周榜+月榜）
     */
    private List<Leaderboard> resolveLeaderboardPlansByMetricIds(String tenantId, List<Long> metricIds) {
        List<LeaderboardMetric> matched = leaderboardMetricMapper.findByMetricIds(metricIds);

        // 按leaderboardId分组，找出包含所有上报指标的计划
        Map<Long, Set<Long>> planMetrics = new HashMap<>();
        for (LeaderboardMetric lm : matched) {
            planMetrics.computeIfAbsent(lm.getLeaderboardId(), k -> new HashSet<>()).add(lm.getMetricId());
        }

        List<Long> matchingPlanIds = new ArrayList<>();
        for (var entry : planMetrics.entrySet()) {
            if (entry.getValue().containsAll(metricIds)) {
                matchingPlanIds.add(entry.getKey());
            }
        }

        // 验证这些计划属于该租户、active且时间有效
        List<Leaderboard> validPlans = new ArrayList<>();
        for (Long planId : matchingPlanIds) {
            Leaderboard lb = leaderboardMapper.findById(planId);
            if (lb == null || !"active".equals(lb.getStatus())) {
                continue;
            }
            if (!lb.getTenantId().equals(tenantId)) {
                continue;
            }
            if (lb.getStartTime() > System.currentTimeMillis()) {
                continue;
            }
            if (lb.getEndTime() != null && lb.getEndTime() <= System.currentTimeMillis()) {
                continue;
            }
            validPlans.add(lb);
        }

        if (validPlans.isEmpty()) {
            throw new BusinessException("上报的指标集合未关联到任何活跃的排行榜计划");
        }

        return validPlans;
    }

    private void writeMetrics(String tenantId, Long leaderboardId, Long instanceId,
                               String userId, List<MetricValue> metrics,
                               Map<String, Long> metricIdMap) {
        List<ScoreRecord> setRecords = new ArrayList<>();
        for (MetricValue mv : metrics) {
            Long internalMetricId = metricIdMap.get(mv.getMetricId());
            String rankingKey = String.format(RANKING_KEY_PREFIX, leaderboardId, instanceId, internalMetricId);

            if (mv.isIncrement()) {
                // 增量模式：Redis ZINCRBY + MySQL ON DUPLICATE KEY UPDATE
                redisTemplate.opsForZSet().incrementScore(rankingKey, userId, mv.getValue().doubleValue());

                ScoreRecord r = new ScoreRecord();
                r.setTenantId(tenantId);
                r.setLeaderboardId(leaderboardId);
                r.setInstanceId(instanceId);
                r.setMetricId(internalMetricId);
                r.setUserId(userId);
                r.setScore(mv.getValue());
                r.setPayload(mv.getPayload());
                scoreRecordMapper.insertOrIncrement(r);
            } else {
                // 覆盖模式：Redis ZADD + MySQL INSERT
                redisTemplate.opsForZSet().add(rankingKey, userId, mv.getValue().doubleValue());

                ScoreRecord r = new ScoreRecord();
                r.setTenantId(tenantId);
                r.setLeaderboardId(leaderboardId);
                r.setInstanceId(instanceId);
                r.setMetricId(internalMetricId);
                r.setUserId(userId);
                r.setScore(mv.getValue());
                r.setPayload(mv.getPayload());
                setRecords.add(r);
            }
        }
        if (!setRecords.isEmpty()) {
            try {
                scoreRecordMapper.insertBatch(setRecords);
            } catch (DuplicateKeyException e) {
                throw new BusinessException("该用户已上报过分数，不允许重复上报");
            }
        }
    }

    /** 判断所有指标是否都是增量模式 */
    private boolean isAllIncrement(List<MetricValue> metrics) {
        return metrics != null && metrics.stream().allMatch(MetricValue::isIncrement);
    }

    private LeaderboardInstance validateAndGetActiveInstance(Long leaderboardId) {
        LeaderboardInstance instance = instanceMapper.findActiveByLeaderboardId(leaderboardId);
        if (instance == null) {
            throw new BusinessException("当前没有活跃的排行榜实例");
        }
        return instance;
    }

    private void validateMetricsAgainstPlan(Long leaderboardId, List<MetricValue> metrics,
                                            Map<String, Long> metricIdMap) {
        List<LeaderboardMetric> lbMetrics = leaderboardMetricMapper.findByLeaderboardId(leaderboardId);
        Set<Long> planMetricIds = lbMetrics.stream().map(LeaderboardMetric::getMetricId).collect(Collectors.toSet());
        for (MetricValue mv : metrics) {
            Long internalMetricId = metricIdMap.get(mv.getMetricId());
            if (!planMetricIds.contains(internalMetricId)) {
                throw new BusinessException("指标 " + mv.getMetricId() + " 不属于该排行榜计划");
            }
        }
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
        newInstance.setInstanceId("li_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
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
     * 单个指标值，payload 在此层级
     */
    public static class MetricValue {
        private String metricId;
        private BigDecimal value;
        private String payload;
        private String mode;

        public String getMetricId() { return metricId; }
        public void setMetricId(String metricId) { this.metricId = metricId; }
        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal value) { this.value = value; }
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public boolean isIncrement() { return "inc".equals(mode); }
    }

    /**
     * 批量上报中的单条数据
     */
    public static class BatchSubmitItem {
        private String userId;
        private List<MetricValue> metrics;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public List<MetricValue> getMetrics() { return metrics; }
        public void setMetrics(List<MetricValue> metrics) { this.metrics = metrics; }
    }
}
