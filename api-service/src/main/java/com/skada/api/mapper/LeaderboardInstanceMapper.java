package com.skada.api.mapper;

import com.skada.api.model.LeaderboardInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LeaderboardInstanceMapper {

    int insert(LeaderboardInstance instance);

    LeaderboardInstance findActiveByLeaderboardId(@Param("leaderboardId") Long leaderboardId);

    LeaderboardInstance findById(@Param("id") Long id);

    LeaderboardInstance findByInstanceId(@Param("instanceId") String instanceId);

    int getMaxInstanceSeq(@Param("leaderboardId") Long leaderboardId);

    List<LeaderboardInstance> findByLeaderboardId(@Param("leaderboardId") Long leaderboardId);

    int closeInstance(@Param("id") Long id, @Param("endTime") Long endTime);
}
