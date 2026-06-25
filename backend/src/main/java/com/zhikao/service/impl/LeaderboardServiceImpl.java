package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.Leaderboard;
import com.zhikao.entity.LeaderboardRecord;
import com.zhikao.entity.PkTeam;
import com.zhikao.entity.UserPoints;
import com.zhikao.mapper.LeaderboardMapper;
import com.zhikao.mapper.LeaderboardRecordMapper;
import com.zhikao.mapper.PkTeamMapper;
import com.zhikao.mapper.UserPointsMapper;
import com.zhikao.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 排行榜Service实现类
 */
@Service
public class LeaderboardServiceImpl
        extends ServiceImpl<LeaderboardMapper, Leaderboard>
        implements LeaderboardService {

    @Autowired
    private LeaderboardMapper leaderboardMapper;

    @Autowired
    private LeaderboardRecordMapper leaderboardRecordMapper;

    @Autowired
    private UserPointsMapper userPointsMapper;

    @Autowired
    private PkTeamMapper pkTeamMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshLeaderboard(Integer leaderboardType, Integer periodType) {
        if (leaderboardType == null) leaderboardType = 1;
        if (periodType == null) periodType = 0;

        switch (leaderboardType) {
            case 1 -> refreshUserTotalPoints(periodType);
            case 2 -> refreshUserWinRate(periodType);
            case 3 -> refreshUserStreak(periodType);
            case 4 -> refreshTeamTotalScore(periodType);
            case 5 -> refreshTeamWinRate(periodType);
            default -> throw new RuntimeException("不支持的排行榜类型: " + leaderboardType);
        }
    }

    /**
     * 个人总积分榜（类型1）：从 user_points.total_points 聚合
     */
    private void refreshUserTotalPoints(Integer periodType) {
        LambdaQueryWrapper<UserPoints> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(UserPoints::getTotalPoints);
        List<UserPoints> list = userPointsMapper.selectList(wrapper);
        int rank = 1;
        for (UserPoints up : list) {
            if (up.getTotalPoints() == null || up.getTotalPoints() <= 0) {
                rank++;
                continue;
            }
            upsertLeaderboard(1, periodType, up.getUserId(), null,
                    up.getTotalPoints(), rank);
            rank++;
        }
    }

    /**
     * 个人胜率榜（类型2）：从 pk_match_record 聚合（按正确率简化）
     */
    private void refreshUserWinRate(Integer periodType) {
        // 暂以 user_points 等级经验作为近似排名依据（避免复杂聚合）
        LambdaQueryWrapper<UserPoints> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(UserPoints::getExperience);
        List<UserPoints> list = userPointsMapper.selectList(wrapper);
        int rank = 1;
        for (UserPoints up : list) {
            if (up.getExperience() == null || up.getExperience() <= 0) {
                rank++;
                continue;
            }
            upsertLeaderboard(2, periodType, up.getUserId(), null,
                    up.getExperience(), rank);
            rank++;
        }
    }

    /**
     * 个人连胜榜（类型3）：暂以连续签到天数近似
     */
    private void refreshUserStreak(Integer periodType) {
        LambdaQueryWrapper<UserPoints> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(UserPoints::getCheckinStreak);
        List<UserPoints> list = userPointsMapper.selectList(wrapper);
        int rank = 1;
        for (UserPoints up : list) {
            if (up.getCheckinStreak() == null || up.getCheckinStreak() <= 0) {
                rank++;
                continue;
            }
            upsertLeaderboard(3, periodType, up.getUserId(), null,
                    up.getCheckinStreak(), rank);
            rank++;
        }
    }

    /**
     * 战队积分榜（类型4）：从 pk_team.total_score 聚合
     */
    private void refreshTeamTotalScore(Integer periodType) {
        LambdaQueryWrapper<PkTeam> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PkTeam::getStatus, 1).orderByDesc(PkTeam::getTotalScore);
        List<PkTeam> list = pkTeamMapper.selectList(wrapper);
        int rank = 1;
        for (PkTeam t : list) {
            if (t.getTotalScore() == null || t.getTotalScore() <= 0) {
                rank++;
                continue;
            }
            upsertLeaderboard(4, periodType, null, t.getId(),
                    t.getTotalScore(), rank);
            rank++;
        }
    }

    /**
     * 战队胜率榜（类型5）：从 pk_team win_count/lose_count 计算
     */
    private void refreshTeamWinRate(Integer periodType) {
        LambdaQueryWrapper<PkTeam> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PkTeam::getStatus, 1);
        List<PkTeam> list = pkTeamMapper.selectList(wrapper);
        list.sort((a, b) -> Integer.compare(calcWinRate(b), calcWinRate(a)));
        int rank = 1;
        for (PkTeam t : list) {
            int rate = calcWinRate(t);
            if (rate <= 0) {
                rank++;
                continue;
            }
            upsertLeaderboard(5, periodType, null, t.getId(), rate, rank);
            rank++;
        }
    }

    private int calcWinRate(PkTeam t) {
        int win = t.getWinCount() == null ? 0 : t.getWinCount();
        int lose = t.getLoseCount() == null ? 0 : t.getLoseCount();
        int total = win + lose;
        return total == 0 ? 0 : (int) (win * 100.0 / total);
    }

    /**
     * 插入或更新排行榜行，并写入 rankNum
     */
    private void upsertLeaderboard(Integer leaderboardType, Integer periodType,
                                   Long userId, Long teamId, Integer score, int rankNum) {
        LambdaQueryWrapper<Leaderboard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Leaderboard::getLeaderboardType, leaderboardType)
               .eq(Leaderboard::getPeriodType, periodType);
        if (userId != null) {
            wrapper.eq(Leaderboard::getUserId, userId);
        } else {
            wrapper.isNull(Leaderboard::getUserId);
        }
        if (teamId != null) {
            wrapper.eq(Leaderboard::getTeamId, teamId);
        } else {
            wrapper.isNull(Leaderboard::getTeamId);
        }
        Leaderboard lb = leaderboardMapper.selectOne(wrapper);
        if (lb == null) {
            lb = new Leaderboard();
            lb.setLeaderboardType(leaderboardType);
            lb.setPeriodType(periodType);
            lb.setUserId(userId);
            lb.setTeamId(teamId);
            lb.setScore(score);
            lb.setRankNum(rankNum);
            lb.setUpdatedAt(new Date());
            leaderboardMapper.insert(lb);
        } else {
            lb.setScore(score);
            lb.setRankNum(rankNum);
            lb.setUpdatedAt(new Date());
            leaderboardMapper.updateById(lb);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archiveSeason(String seasonKey) {
        if (seasonKey == null || seasonKey.isBlank()) {
            throw new RuntimeException("赛季标识不能为空");
        }
        // 快照个人总榜（type=1, period=0）
        LambdaQueryWrapper<Leaderboard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Leaderboard::getLeaderboardType, 1)
               .eq(Leaderboard::getPeriodType, 0)
               .orderByAsc(Leaderboard::getRankNum);
        List<Leaderboard> list = leaderboardMapper.selectList(wrapper);
        for (Leaderboard lb : list) {
            LeaderboardRecord record = new LeaderboardRecord();
            record.setLeaderboardId(lb.getId());
            record.setUserId(lb.getUserId());
            record.setTeamId(lb.getTeamId());
            record.setRankNum(lb.getRankNum());
            record.setScore(lb.getScore());
            record.setRewardStatus(0);
            record.setSeasonKey(seasonKey);
            record.setCreatedAt(new Date());
            leaderboardRecordMapper.insert(record);
        }
    }

    @Override
    public List<LeaderboardRecord> listSeasonRecords(String seasonKey, Integer leaderboardType) {
        return leaderboardRecordMapper.selectBySeasonKey(seasonKey, leaderboardType);
    }
}
