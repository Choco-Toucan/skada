package com.skada.api.mapper;

import com.skada.api.model.ScoreRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ScoreRecordMapper {

    int insert(ScoreRecord record);

    int insertBatch(List<ScoreRecord> records);

    List<ScoreRecord> findRanking(@Param("leaderboardId") Long leaderboardId,
                                  @Param("cycleId") Long cycleId,
                                  @Param("sortOrder") String sortOrder,
                                  @Param("limit") int limit);

    ScoreRecord findByUserAndCycle(@Param("leaderboardId") Long leaderboardId,
                                   @Param("cycleId") Long cycleId,
                                   @Param("userId") String userId);

    int countByCycle(@Param("leaderboardId") Long leaderboardId,
                     @Param("cycleId") Long cycleId);

    int countDistinctUsersByCycle(@Param("leaderboardId") Long leaderboardId,
                                  @Param("cycleId") Long cycleId);
}
