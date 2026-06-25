package com.zhikao.controller;

import com.zhikao.common.Result;
import com.zhikao.entity.LeaderboardRecord;
import com.zhikao.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 排行榜刷新与赛季历史控制器
 */
@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {

    @Autowired
    private LeaderboardService leaderboardService;

    /**
     * 刷新排行榜（仅超级管理员）
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/refresh")
    public Result<?> refresh(@RequestBody Map<String, Integer> body) {
        Integer leaderboardType = body.get("leaderboardType");
        Integer periodType = body.get("periodType");
        leaderboardService.refreshLeaderboard(leaderboardType, periodType);
        return Result.success("排行榜已刷新");
    }

    /**
     * 查询赛季历史记录
     */
    @GetMapping("/season/{seasonKey}")
    public Result<List<LeaderboardRecord>> seasonRecords(
            @PathVariable String seasonKey,
            @RequestParam(required = false) Integer leaderboardType) {
        return Result.success(leaderboardService.listSeasonRecords(seasonKey, leaderboardType));
    }

    /**
     * 归档当前总榜为赛季快照（仅超级管理员）
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/archive")
    public Result<?> archive(@RequestBody Map<String, String> body) {
        String seasonKey = body.get("seasonKey");
        leaderboardService.archiveSeason(seasonKey);
        return Result.success("赛季已归档");
    }
}
