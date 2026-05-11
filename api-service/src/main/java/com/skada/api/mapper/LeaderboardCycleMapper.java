package com.skada.api.mapper;

import com.skada.api.model.LeaderboardCycle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LeaderboardCycleMapper {

    int insert(LeaderboardCycle cycle);

    LeaderboardCycle findActiveByLeaderboardId(@Param("leaderboardId") Long leaderboardId);

    LeaderboardCycle findById(@Param("id") Long id);

    int closeCycle(@Param("id") Long id, @Param("endTime") Long endTime);

    int getMaxCycleSeq(@Param("leaderboardId") Long leaderboardId);
}
