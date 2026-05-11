package com.skada.api.service;

import com.skada.api.mapper.LeaderboardCycleMapper;
import com.skada.api.mapper.LeaderboardMapper;
import com.skada.api.mapper.ScoreRecordMapper;
import com.skada.api.model.Leaderboard;
import com.skada.api.model.LeaderboardCycle;
import com.skada.api.model.ScoreRecord;
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
    private final StringRedisTemplate redisTemplate;

    public LeaderboardQueryService(LeaderboardMapper leaderboardMapper,
                                   LeaderboardCycleMapper cycleMapper,
                                   ScoreRecordMapper scoreRecordMapper,
                                   StringRedisTemplate redisTemplate) {
        this.leaderboardMapper = leaderboardMapper;
        this.cycleMapper = cycleMapper;
        this.scoreRecordMapper = scoreRecordMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 查询排行榜排名
     */
    public List<RankEntry> getRanking(Long leaderboardId, Long cycleId, int limit) {
        Leaderboard lb = leaderboardMapper.findById(leaderboardId);
        if (lb == null) {
            throw new BusinessException("排行榜不存在");
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

        // Redis无数据，从MySQL读取（兜底）
        List<ScoreRecord> dbRecords = scoreRecordMapper.findRanking(
                leaderboardId, cycleId, lb.getSortOrder(), queryLimit);

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
