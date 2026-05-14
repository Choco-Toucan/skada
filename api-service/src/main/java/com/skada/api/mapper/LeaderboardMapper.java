package com.skada.api.mapper;

import com.skada.api.model.Leaderboard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LeaderboardMapper {

    Leaderboard findById(@Param("id") Long id);

    Leaderboard findByPlanId(@Param("planId") String planId);

    int update(Leaderboard leaderboard);
}
