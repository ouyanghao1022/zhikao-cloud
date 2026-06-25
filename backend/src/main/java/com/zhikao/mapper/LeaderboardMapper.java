package com.zhikao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhikao.entity.Leaderboard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 排行榜Mapper
 */
@Mapper
public interface LeaderboardMapper extends BaseMapper<Leaderboard> {

    /**
     * 查询排行榜（含用户/队伍信息）
     */
    @Select("<script>" +
            "SELECT lb.*, u.username, u.nickname, u.avatar, t.team_name as teamName " +
            "FROM leaderboard lb " +
            "LEFT JOIN sys_user u ON lb.user_id = u.id " +
            "LEFT JOIN pk_team t ON lb.team_id = t.id " +
            "WHERE lb.leaderboard_type = #{type} " +
            "<if test='period != null and period != 0'>AND lb.period_type = #{period}</if> " +
            "ORDER BY lb.rank_num ASC " +
            "LIMIT #{limit}" +
            "</script>")
    List<Leaderboard> selectLeaderboard(@Param("type") Integer type,
                                          @Param("period") Integer period,
                                          @Param("limit") Integer limit);
}
