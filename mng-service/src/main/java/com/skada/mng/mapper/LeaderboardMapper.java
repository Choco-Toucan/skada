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

    List<Leaderboard> findByTenantIdWithPage(@Param("tenantId") String tenantId,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit);

    long countByTenantId(@Param("tenantId") String tenantId);

    List<Leaderboard> findAll();

    List<Leaderboard> findAllWithPage(@Param("offset") int offset, @Param("limit") int limit);

    long count();
}
