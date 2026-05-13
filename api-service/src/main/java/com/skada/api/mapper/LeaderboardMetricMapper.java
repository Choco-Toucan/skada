package com.skada.api.mapper;

import com.skada.api.model.LeaderboardMetric;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LeaderboardMetricMapper {

    List<LeaderboardMetric> findByLeaderboardId(@Param("leaderboardId") Long leaderboardId);

    List<LeaderboardMetric> findByMetricIds(@Param("metricIds") List<Long> metricIds);
}
