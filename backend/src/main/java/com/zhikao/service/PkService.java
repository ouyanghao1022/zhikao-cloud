package com.zhikao.service;

import com.zhikao.entity.*;

import java.util.List;

/**
 * PK与排行榜Service接口
 */
public interface PkService {

    /** 创建队伍 */
    PkTeam createTeam(Long userId, PkTeam team);

    /** 加入队伍 */
    void joinTeam(Long userId, Long teamId);

    /** 退出队伍 */
    void leaveTeam(Long userId, Long teamId);

    /** 队伍列表 */
    List<PkTeam> listTeams();

    /** 发起对战（指定对手或随机匹配） */
    PkMatch startMatch(Long userId, PkMatch match);

    /** 获取对战详情 */
    PkMatch getMatchDetail(Long matchId);

    /** 获取对战答题记录 */
    List<PkMatchRecord> getMatchRecords(Long matchId);

    /** 提交对战答案 */
    PkMatchRecord submitAnswer(Long userId, PkMatchRecord record);

    /** 结束对战，宣布胜负 */
    PkMatch endMatch(Long matchId, Long userId);

    /** 排行榜 */
    List<Leaderboard> getLeaderboard(Integer type, Integer period);

    /** 获取用户当前队伍 */
    PkTeam getMyTeam(Long userId);

    /** 周榜Top10 */
    List<Leaderboard> getTopLeaderboard();

    /** 解散队伍（管理员） */
    void dismissTeam(Long teamId);

    /** 队伍成员列表 */
    List<PkTeamMember> listTeamMembers(Long teamId);

    /** 队伍对战记录 */
    List<PkMatch> listTeamMatches(Long teamId);

    /** 编辑队伍信息（管理员） */
    void updateTeam(Long teamId, PkTeam updates);

    /** 从队伍中移除成员（管理员） */
    void removeTeamMember(Long teamId, Long userId);

    /** 查看所有对战记录（管理员） */
    List<PkMatch> listAllMatches();

    /** 取消/终止对战（管理员） */
    void cancelMatch(Long matchId);
}
