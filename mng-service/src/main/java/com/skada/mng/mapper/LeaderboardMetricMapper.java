package com.skada.mng.mapper;

import com.skada.mng.model.LeaderboardMetric;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LeaderboardMetricMapper {

    int insert(LeaderboardMetric lm);

    int insertBatch(@Param("list") List<LeaderboardMetric> list);

    int deleteByLeaderboardId(@Param("leaderboardId") Long leaderboardId);

    List<LeaderboardMetric> findByLeaderboardId(@Param("leaderboardId") Long leaderboardId);
}
