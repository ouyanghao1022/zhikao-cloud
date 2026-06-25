package com.zhikao.controller;

import com.zhikao.common.Result;
import com.zhikao.entity.*;
import com.zhikao.service.PkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PK与排行榜控制器
 */
@RestController
@RequestMapping("/pk")
public class PkController {

    @Autowired
    private PkService pkService;

    /**
     * 队伍列表
     */
    @GetMapping("/teams")
    public Result<List<PkTeam>> teams() {
        List<PkTeam> teams = pkService.listTeams();
        return Result.success(teams);
    }

    /**
     * 获取当前用户的队伍
     */
    @GetMapping("/teams/my")
    public Result<PkTeam> myTeam(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        PkTeam team = pkService.getMyTeam(userId);
        return Result.success(team);
    }

    /**
     * 创建队伍
     */
    @PostMapping("/team")
    public Result<PkTeam> createTeam(@RequestBody PkTeam team, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        PkTeam created = pkService.createTeam(userId, team);
        return Result.success("创建成功", created);
    }

    /**
     * 加入队伍
     */
    @PostMapping("/team/join")
    public Result<?> joinTeam(@RequestBody PkTeamMember member, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        pkService.joinTeam(userId, member.getTeamId());
        return Result.success("加入成功");
    }

    /**
     * 退出队伍
     */
    @PostMapping("/team/leave")
    public Result<?> leaveTeam(@RequestBody PkTeamMember member, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        pkService.leaveTeam(userId, member.getTeamId());
        return Result.success("退出成功");
    }

    /**
     * 发起对战
     */
    @PostMapping("/match/start")
    public Result<PkMatch> startMatch(@RequestBody PkMatch match, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        PkMatch created = pkService.startMatch(userId, match);
        return Result.success("对战已创建", created);
    }

    /**
     * 对战详情
     */
    @GetMapping("/match/{id}")
    public Result<PkMatch> matchDetail(@PathVariable Long id) {
        PkMatch match = pkService.getMatchDetail(id);
        if (match == null) {
            return Result.error("对战不存在");
        }
        return Result.success(match);
    }

    /**
     * 对战答题记录
     */
    @GetMapping("/match/{id}/records")
    public Result<List<PkMatchRecord>> matchRecords(@PathVariable Long id) {
        List<PkMatchRecord> records = pkService.getMatchRecords(id);
        return Result.success(records);
    }

    /**
     * 提交对战答案
     */
    @PostMapping("/match/answer")
    public Result<PkMatchRecord> submitAnswer(@RequestBody PkMatchRecord record, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        PkMatchRecord saved = pkService.submitAnswer(userId, record);
        return Result.success("提交成功", saved);
    }

    /**
     * 结束对战
     */
    @PostMapping("/match/{id}/end")
    public Result<PkMatch> endMatch(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        PkMatch match = pkService.endMatch(id, userId);
        return Result.success("对战已结束", match);
    }

    /**
     * 排行榜
     */
    @GetMapping("/leaderboard")
    public Result<List<Leaderboard>> leaderboard(
            @RequestParam(defaultValue = "1") Integer type,
            @RequestParam(defaultValue = "0") Integer period) {
        List<Leaderboard> list = pkService.getLeaderboard(type, period);
        return Result.success(list);
    }

    /**
     * 周榜Top10
     */
    @GetMapping("/leaderboard/top")
    public Result<List<Leaderboard>> leaderboardTop() {
        List<Leaderboard> list = pkService.getTopLeaderboard();
        return Result.success(list);
    }

    /**
     * 解散队伍（管理员）
     */
    @DeleteMapping("/team/{id}")
    public Result<?> dismissTeam(@PathVariable Long id) {
        pkService.dismissTeam(id);
        return Result.success("已解散");
    }

    /**
     * 队伍成员列表
     */
    @GetMapping("/team/{id}/members")
    public Result<List<PkTeamMember>> teamMembers(@PathVariable Long id) {
        List<PkTeamMember> members = pkService.listTeamMembers(id);
        return Result.success(members);
    }

    /**
     * 队伍对战记录
     */
    @GetMapping("/team/{id}/matches")
    public Result<List<PkMatch>> teamMatches(@PathVariable Long id) {
        List<PkMatch> matches = pkService.listTeamMatches(id);
        return Result.success(matches);
    }

    // ==================== 管理端接口 ====================

    /**
     * 编辑队伍信息（管理员）
     */
    @PutMapping("/team/{id}")
    public Result<?> updateTeam(@PathVariable Long id, @RequestBody PkTeam updates) {
        pkService.updateTeam(id, updates);
        return Result.success("保存成功");
    }

    /**
     * 从队伍中移除成员（管理员）
     */
    @DeleteMapping("/team/{teamId}/member/{userId}")
    public Result<?> removeTeamMember(@PathVariable Long teamId, @PathVariable Long userId) {
        pkService.removeTeamMember(teamId, userId);
        return Result.success("已移除");
    }

    /**
     * 查看所有对战记录（管理员）
     */
    @GetMapping("/matches")
    public Result<List<PkMatch>> allMatches() {
        List<PkMatch> matches = pkService.listAllMatches();
        return Result.success(matches);
    }

    /**
     * 取消/终止对战（管理员）
     */
    @DeleteMapping("/match/{id}")
    public Result<?> cancelMatch(@PathVariable Long id) {
        pkService.cancelMatch(id);
        return Result.success("对战已取消");
    }
}
