package com.zhikao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhikao.entity.LeaderboardRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 排行榜历史记录Mapper
 */
@Mapper
public interface LeaderboardRecordMapper extends BaseMapper<LeaderboardRecord> {

    /**
     * 按赛季标识查询历史记录，可选按排行榜类型过滤（通过 JOIN leaderboard 取类型）
     */
    @Select("<script>" +
            "SELECT lr.* FROM leaderboard_record lr " +
            "INNER JOIN leaderboard lb ON lr.leaderboard_id = lb.id " +
            "WHERE lr.season_key = #{seasonKey} " +
            "<if test='leaderboardType != null'>AND lb.leaderboard_type = #{leaderboardType}</if> " +
            "ORDER BY lr.rank_num ASC" +
            "</script>")
    List<LeaderboardRecord> selectBySeasonKey(@Param("seasonKey") String seasonKey,
                                              @Param("leaderboardType") Integer leaderboardType);
}

