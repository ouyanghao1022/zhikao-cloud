package com.zhikao.service;

import com.zhikao.entity.LeaderboardRecord;

import java.util.List;

/**
 * 排行榜Service接口
 */
public interface LeaderboardService {

    /**
     * 刷新排行榜
     *
     * @param leaderboardType 排行榜类型：1个人总积分 2个人胜率 3个人连胜 4战队积分 5战队胜率
     * @param periodType      周期类型：0总榜 1周榜 2月榜 3赛季榜
     */
    void refreshLeaderboard(Integer leaderboardType, Integer periodType);

    /**
     * 将当前总榜快照到 leaderboard_record（rewardStatus=0）
     *
     * @param seasonKey 赛季标识
     */
    void archiveSeason(String seasonKey);

    /**
     * 查询赛季历史
     *
     * @param seasonKey        赛季标识
     * @param leaderboardType  排行榜类型，可空
     */
    List<LeaderboardRecord> listSeasonRecords(String seasonKey, Integer leaderboardType);
}
