package com.skada.mng.mapper;

import com.skada.mng.model.LeaderboardCycle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LeaderboardCycleMapper {

    int insert(LeaderboardCycle cycle);

    LeaderboardCycle findById(@Param("id") Long id);

    LeaderboardCycle findActiveByLeaderboardId(@Param("leaderboardId") Long leaderboardId);

    List<LeaderboardCycle> findByLeaderboardId(@Param("leaderboardId") Long leaderboardId);

    int closeCycle(@Param("id") Long id, @Param("endTime") Long endTime);

    int getMaxCycleSeq(@Param("leaderboardId") Long leaderboardId);
}
