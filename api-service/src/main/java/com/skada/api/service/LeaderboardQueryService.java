package com.skada.api.service;

import com.skada.api.mapper.LeaderboardCycleMapper;
import com.skada.api.mapper.LeaderboardMapper;
import com.skada.api.mapper.ScoreRecordMapper;
import com.skada.api.mapper.TenantMapper;
import com.skada.api.model.Leaderboard;
import com.skada.api.model.LeaderboardCycle;
import com.skada.api.model.ScoreRecord;
import com.skada.api.model.Tenant;
import com.skada.common.exception.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 排行榜查询服务
 * 优先从Redis ZSET读取，降级到MySQL
 */
@Service
public class LeaderboardQueryService {

    private static final String RANKING_KEY_PREFIX = "skada:leaderboard:%d:cycle:%d";
    private static final long CACHE_TTL_SECONDS = 600; // 10分钟

    private final LeaderboardMapper leaderboardMapper;
    private final LeaderboardCycleMapper cycleMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final TenantMapper tenantMapper;
    private final StringRedisTemplate redisTemplate;

    public LeaderboardQueryService(LeaderboardMapper leaderboardMapper,
                                   LeaderboardCycleMapper cycleMapper,
                                   ScoreRecordMapper scoreRecordMapper,
                                   TenantMapper tenantMapper,
                                   StringRedisTemplate redisTemplate) {
        this.leaderboardMapper = leaderboardMapper;
        this.cycleMapper = cycleMapper;
        this.scoreRecordMapper = scoreRecordMapper;
        this.tenantMapper = tenantMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 查询排行榜排名
     * @param tenantId 租户ID（可选，仅当租户不允许匿名查询时必填）
     * @param secretKey 租户密钥（可选，仅当租户不允许匿名查询时必填）
     */
    public List<RankEntry> getRanking(Long leaderboardId, Long cycleId, int limit,
                                       String tenantId, String secretKey) {
        Leaderboard lb = leaderboardMapper.findById(leaderboardId);
        if (lb == null) {
            throw new BusinessException("排行榜不存在");
        }

        // 租户查询鉴权：检查是否允许匿名查询
        Tenant tenant = tenantMapper.findByTenantId(lb.getTenantId());
        if (tenant == null || tenant.getStatus() != 1) {
            throw new BusinessException("租户不存在或已停用");
        }
        if (tenant.getAllowAnonymousQuery() == 0) {
            // 不允许匿名查询，必须提供有效凭证
            if (tenantId == null || secretKey == null) {
                throw new BusinessException(401, "该排行榜不允许匿名查询，请提供租户凭证");
            }
            if (!tenant.getTenantId().equals(tenantId) || !tenant.getSecretKey().equals(secretKey)) {
                throw new BusinessException(401, "租户凭证无效");
            }
        }

        if (cycleId == null) {
            LeaderboardCycle active = cycleMapper.findActiveByLeaderboardId(leaderboardId);
            if (active == null) {
                throw new BusinessException("当前没有活跃周期");
            }
            cycleId = active.getId();
        }

        int queryLimit = Math.min(limit, lb.getMaxQueryUsers());
        String rankingKey = String.format(RANKING_KEY_PREFIX, leaderboardId, cycleId);

        // 安全校验排序方向，仅允许 asc/desc，防止 SQL 注入
        String sortOrder = "asc".equals(lb.getSortOrder()) ? "ASC" : "DESC";

        // 优先从Redis读取
        Set<ZSetOperations.TypedTuple<String>> redisResult;
        if ("asc".equals(lb.getSortOrder())) {
            redisResult = redisTemplate.opsForZSet()
                    .rangeWithScores(rankingKey, 0, queryLimit - 1);
        } else {
            redisResult = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(rankingKey, 0, queryLimit - 1);
        }

        if (redisResult != null && !redisResult.isEmpty()) {
            List<RankEntry> result = new ArrayList<>();
            int rank = 1;
            for (ZSetOperations.TypedTuple<String> tuple : redisResult) {
                result.add(new RankEntry(rank++, tuple.getValue(),
                        BigDecimal.valueOf(tuple.getScore() != null ? tuple.getScore() : 0)));
            }
            return result;
        }

        // Redis无数据，从MySQL读取（兜底），sortOrder已在Java层校验为 ASC/DESC
        List<ScoreRecord> dbRecords = scoreRecordMapper.findRanking(
                leaderboardId, cycleId, sortOrder, queryLimit);

        // 将MySQL数据回填Redis
        if (!dbRecords.isEmpty()) {
            for (ScoreRecord r : dbRecords) {
                redisTemplate.opsForZSet()
                        .add(rankingKey, r.getUserId(), r.getScore().doubleValue());
            }
            redisTemplate.expire(rankingKey, java.time.Duration.ofSeconds(CACHE_TTL_SECONDS));
        }

        List<RankEntry> result = new ArrayList<>();
        int rank = 1;
        for (ScoreRecord r : dbRecords) {
            result.add(new RankEntry(rank++, r.getUserId(), r.getScore()));
        }
        return result;
    }

    /**
     * 查询排行榜的所有周期（含历史周期）
     */
    public List<LeaderboardCycle> getCycles(Long leaderboardId) {
        Leaderboard lb = leaderboardMapper.findById(leaderboardId);
        if (lb == null) {
            throw new BusinessException("排行榜不存在");
        }
        return cycleMapper.findByLeaderboardId(leaderboardId);
    }

    /**
     * 排名条目
     */
    public static class RankEntry {
        private int rank;
        private String userId;
        private BigDecimal score;

        public RankEntry(int rank, String userId, BigDecimal score) {
            this.rank = rank;
            this.userId = userId;
            this.score = score;
        }

        public int getRank() { return rank; }
        public String getUserId() { return userId; }
        public BigDecimal getScore() { return score; }
    }
}
