package com.skada.api.service;

import com.skada.api.mapper.*;
import com.skada.api.model.*;
import com.skada.common.exception.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 排行榜查询服务
 * 支持多指标排序：按指标优先级依次比较，每个指标独立ZSET缓存
 */
@Service
public class LeaderboardQueryService {

    private static final String RANKING_KEY_PREFIX = "skada:metric:%d:%d:%d";
    private static final long CACHE_TTL_SECONDS = 600;

    private final LeaderboardMapper leaderboardMapper;
    private final LeaderboardInstanceMapper instanceMapper;
    private final LeaderboardMetricMapper leaderboardMetricMapper;
    private final MetricMapper metricMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final TenantMapper tenantMapper;
    private final StringRedisTemplate redisTemplate;

    public LeaderboardQueryService(LeaderboardMapper leaderboardMapper,
                                   LeaderboardInstanceMapper instanceMapper,
                                   LeaderboardMetricMapper leaderboardMetricMapper,
                                   MetricMapper metricMapper,
                                   ScoreRecordMapper scoreRecordMapper,
                                   TenantMapper tenantMapper,
                                   StringRedisTemplate redisTemplate) {
        this.leaderboardMapper = leaderboardMapper;
        this.instanceMapper = instanceMapper;
        this.leaderboardMetricMapper = leaderboardMetricMapper;
        this.metricMapper = metricMapper;
        this.scoreRecordMapper = scoreRecordMapper;
        this.tenantMapper = tenantMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 查询排行榜排名（多指标）
     */
    public List<RankEntry> getRanking(String planId, String instanceIdStr, int from, int to,
                                       String tenantId, String secretKey) {
        // 参数校验
        if (from < 0 || to < 0 || from > to) {
            throw new BusinessException("分页参数无效: from 必须 ≤ to 且都为非负数");
        }

        // 解析planId → Long leaderboardId
        Leaderboard lb = leaderboardMapper.findByPlanId(planId);
        if (lb == null) {
            throw new BusinessException("排行榜计划不存在");
        }
        Long leaderboardId = lb.getId();

        // 租户鉴权
        Tenant tenant = tenantMapper.findByTenantId(lb.getTenantId());
        if (tenant == null || tenant.getStatus() != 1) {
            throw new BusinessException("租户不存在或已停用");
        }
        if (tenant.getAllowAnonymousQuery() == 0) {
            if (tenantId == null || secretKey == null) {
                throw new BusinessException(401, "该排行榜不允许匿名查询，请提供租户凭证");
            }
            if (!tenant.getTenantId().equals(tenantId) || !tenant.getSecretKey().equals(secretKey)) {
                throw new BusinessException(401, "租户凭证无效");
            }
        }

        // 解析instanceId
        Long instanceId = null;
        if (instanceIdStr != null && !instanceIdStr.isEmpty()) {
            LeaderboardInstance inst = instanceMapper.findByInstanceId(instanceIdStr);
            if (inst == null) {
                throw new BusinessException("实例不存在");
            }
            instanceId = inst.getId();
        } else {
            LeaderboardInstance active = instanceMapper.findActiveByLeaderboardId(leaderboardId);
            if (active == null) {
                throw new BusinessException("当前没有活跃实例");
            }
            instanceId = active.getId();
        }

        // 获取排行榜关联的指标（按优先级排序）
        List<LeaderboardMetric> lbMetrics = leaderboardMetricMapper.findByLeaderboardId(leaderboardId);
        if (lbMetrics.isEmpty()) {
            throw new BusinessException("排行榜计划未关联指标");
        }
        lbMetrics.sort(Comparator.comparingInt(LeaderboardMetric::getPriority));

        // 加载指标名称和外部ID
        List<Long> metricIds = lbMetrics.stream().map(LeaderboardMetric::getMetricId).toList();
        List<Metric> metrics = metricMapper.findByIds(metricIds);
        Map<Long, String> metricNames = new HashMap<>();
        Map<Long, String> metricExternalIds = new HashMap<>();
        for (Metric m : metrics) {
            metricNames.put(m.getId(), m.getName());
            metricExternalIds.put(m.getId(), m.getMetricId());
        }

        // 主指标（最高优先级）用于排序
        LeaderboardMetric primaryMetric = lbMetrics.get(0);
        int size = Math.min(to - from + 1, lb.getMaxQueryUsers() - from);
        if (size <= 0) {
            return List.of();
        }
        String primaryKey = String.format(RANKING_KEY_PREFIX, leaderboardId, instanceId, primaryMetric.getMetricId());

        // 从Redis读取主指标排名
        Set<ZSetOperations.TypedTuple<String>> redisResult;
        if ("asc".equals(primaryMetric.getSortOrder())) {
            redisResult = redisTemplate.opsForZSet().rangeWithScores(primaryKey, from, from + size - 1);
        } else {
            redisResult = redisTemplate.opsForZSet().reverseRangeWithScores(primaryKey, from, from + size - 1);
        }

        if (redisResult != null && !redisResult.isEmpty()) {
            return buildRankEntries(redisResult, leaderboardId, instanceId,
                    lbMetrics, metricNames, metricExternalIds, from);
        }

        // Redis无数据，从MySQL兜底
        String sortOrder = "asc".equals(primaryMetric.getSortOrder()) ? "ASC" : "DESC";
        List<ScoreRecord> dbRecords = scoreRecordMapper.findRanking(
                leaderboardId, instanceId, sortOrder, from, size);

        // 回填Redis
        if (!dbRecords.isEmpty()) {
            Map<Long, Map<String, Double>> metricScores = new HashMap<>();
            for (ScoreRecord r : dbRecords) {
                String key = String.format(RANKING_KEY_PREFIX, leaderboardId, instanceId, r.getMetricId());
                metricScores.computeIfAbsent(r.getMetricId(), k -> new HashMap<>())
                        .put(r.getUserId(), r.getScore().doubleValue());
            }
            for (var entry : metricScores.entrySet()) {
                String key = String.format(RANKING_KEY_PREFIX, leaderboardId, instanceId, entry.getKey());
                for (var ue : entry.getValue().entrySet()) {
                    redisTemplate.opsForZSet().add(key, ue.getKey(), ue.getValue());
                }
                redisTemplate.expire(key, java.time.Duration.ofSeconds(CACHE_TTL_SECONDS));
            }
        }

        return buildRankEntriesFromDb(dbRecords, lbMetrics, metricNames, metricExternalIds, from);
    }

    /**
     * 查询排行榜计划的所有实例
     */
    public List<LeaderboardInstance> getInstances(String planId) {
        Leaderboard lb = leaderboardMapper.findByPlanId(planId);
        if (lb == null) {
            throw new BusinessException("排行榜计划不存在");
        }
        return instanceMapper.findByLeaderboardId(lb.getId());
    }

    private List<RankEntry> buildRankEntries(Set<ZSetOperations.TypedTuple<String>> redisResult,
                                              Long leaderboardId, Long instanceId,
                                              List<LeaderboardMetric> lbMetrics,
                                              Map<Long, String> metricNames,
                                              Map<Long, String> metricExternalIds,
                                              int from) {
        List<UserScore> userScores = new ArrayList<>();
        for (var tuple : redisResult) {
            String userId = tuple.getValue();
            double primaryScore = tuple.getScore() != null ? tuple.getScore() : 0;
            userScores.add(new UserScore(userId, primaryScore));
        }

        // 加载其他指标的值用于返回
        for (int i = 1; i < lbMetrics.size(); i++) {
            LeaderboardMetric lm = lbMetrics.get(i);
            String key = String.format(RANKING_KEY_PREFIX, leaderboardId, instanceId, lm.getMetricId());
            for (var us : userScores) {
                Double val = redisTemplate.opsForZSet().score(key, us.userId);
                us.metricValues.put(lm.getMetricId(), val != null ? BigDecimal.valueOf(val) : BigDecimal.ZERO);
            }
        }

        List<RankEntry> result = new ArrayList<>();
        int rank = from + 1;
        for (var us : userScores) {
            RankEntry entry = new RankEntry();
            entry.setRank(rank++);
            entry.setUserId(us.userId);

            List<MetricValueEntry> metricValues = new ArrayList<>();
            // 主指标值
            MetricValueEntry primary = new MetricValueEntry();
            Long primaryMid = lbMetrics.get(0).getMetricId();
            primary.setMetricId(metricExternalIds.getOrDefault(primaryMid, ""));
            primary.setMetricName(metricNames.get(primaryMid));
            primary.setValue(BigDecimal.valueOf(us.primaryScore));
            metricValues.add(primary);
            // 其他指标值
            for (int i = 1; i < lbMetrics.size(); i++) {
                Long mid = lbMetrics.get(i).getMetricId();
                MetricValueEntry mve = new MetricValueEntry();
                mve.setMetricId(metricExternalIds.getOrDefault(mid, ""));
                mve.setMetricName(metricNames.get(mid));
                mve.setValue(us.metricValues.getOrDefault(mid, BigDecimal.ZERO));
                metricValues.add(mve);
            }
            entry.setMetricValues(metricValues);
            result.add(entry);
        }
        return result;
    }

    private List<RankEntry> buildRankEntriesFromDb(List<ScoreRecord> dbRecords,
                                                    List<LeaderboardMetric> lbMetrics,
                                                    Map<Long, String> metricNames,
                                                    Map<Long, String> metricExternalIds,
                                                    int from) {
        // 按userId聚合
        Map<String, Map<Long, BigDecimal>> userMetricMap = new LinkedHashMap<>();
        for (ScoreRecord r : dbRecords) {
            userMetricMap.computeIfAbsent(r.getUserId(), k -> new HashMap<>())
                    .put(r.getMetricId(), r.getScore());
        }

        List<RankEntry> result = new ArrayList<>();
        int rank = from + 1;
        for (var entry : userMetricMap.entrySet()) {
            RankEntry re = new RankEntry();
            re.setRank(rank++);
            re.setUserId(entry.getKey());
            List<MetricValueEntry> values = new ArrayList<>();
            for (LeaderboardMetric lm : lbMetrics) {
                MetricValueEntry mve = new MetricValueEntry();
                mve.setMetricId(metricExternalIds.getOrDefault(lm.getMetricId(), ""));
                mve.setMetricName(metricNames.get(lm.getMetricId()));
                mve.setValue(entry.getValue().getOrDefault(lm.getMetricId(), BigDecimal.ZERO));
                values.add(mve);
            }
            re.setMetricValues(values);
            result.add(re);
        }
        return result;
    }

    private static class UserScore {
        final String userId;
        final double primaryScore;
        final Map<Long, BigDecimal> metricValues = new HashMap<>();

        UserScore(String userId, double primaryScore) {
            this.userId = userId;
            this.primaryScore = primaryScore;
        }
    }

    /**
     * 排名条目（多指标）
     */
    public static class RankEntry {
        private int rank;
        private String userId;
        private List<MetricValueEntry> metricValues;

        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public List<MetricValueEntry> getMetricValues() { return metricValues; }
        public void setMetricValues(List<MetricValueEntry> metricValues) { this.metricValues = metricValues; }
    }

    public static class MetricValueEntry {
        private String metricId;
        private String metricName;
        private BigDecimal value;

        public String getMetricId() { return metricId; }
        public void setMetricId(String metricId) { this.metricId = metricId; }
        public String getMetricName() { return metricName; }
        public void setMetricName(String metricName) { this.metricName = metricName; }
        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal value) { this.value = value; }
    }
}
