package com.skada.mng.mapper;

import com.skada.mng.model.Leaderboard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LeaderboardMapper {

    int insert(Leaderboard leaderboard);

    int update(Leaderboard leaderboard);

    Leaderboard findById(@Param("id") Long id);

    List<Leaderboard> findByTenantId(@Param("tenantId") String tenantId);

    List<Leaderboard> findAll();
}
