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
                                  @Param("instanceId") Long instanceId,
                                  @Param("metricId") Long metricId,
                                  @Param("sortOrder") String sortOrder,
                                  @Param("from") int from,
                                  @Param("size") int size);

    ScoreRecord findByUserAndInstance(@Param("leaderboardId") Long leaderboardId,
                                      @Param("instanceId") Long instanceId,
                                      @Param("userId") String userId);

    int countByInstance(@Param("leaderboardId") Long leaderboardId,
                        @Param("instanceId") Long instanceId);

    int countDistinctUsersByInstance(@Param("leaderboardId") Long leaderboardId,
                                     @Param("instanceId") Long instanceId);

    List<ScoreRecord> findPayloadsByUsers(@Param("leaderboardId") Long leaderboardId,
                                          @Param("instanceId") Long instanceId,
                                          @Param("userIds") List<String> userIds);
}
