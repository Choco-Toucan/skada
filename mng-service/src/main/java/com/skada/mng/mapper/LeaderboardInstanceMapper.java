package com.skada.mng.mapper;

import com.skada.mng.model.LeaderboardInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LeaderboardInstanceMapper {

    int insert(LeaderboardInstance instance);

    LeaderboardInstance findById(@Param("id") Long id);

    LeaderboardInstance findActiveByLeaderboardId(@Param("leaderboardId") Long leaderboardId);

    List<LeaderboardInstance> findByLeaderboardId(@Param("leaderboardId") Long leaderboardId);

    int closeInstance(@Param("id") Long id, @Param("endTime") Long endTime);

    int getMaxInstanceSeq(@Param("leaderboardId") Long leaderboardId);
}
