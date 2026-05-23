package com.skada.mng.mapper;

import com.skada.mng.model.ScoreRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 分数记录查询（管理后台只读）
 */
@Mapper
public interface ScoreRecordMapper {

    List<ScoreRecord> findByInstance(@Param("leaderboardId") Long leaderboardId,
                                     @Param("instanceId") Long instanceId);

    List<ScoreRecord> findPayloadsByUsers(@Param("leaderboardId") Long leaderboardId,
                                           @Param("instanceId") Long instanceId,
                                           @Param("userIds") List<String> userIds);
}
