package com.zhikao.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhikao.common.PageResult;
import com.zhikao.common.Result;
import com.zhikao.entity.UserPoints;
import com.zhikao.mapper.UserPointsMapper;
import com.zhikao.service.PointsService;
import com.zhikao.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/points")
public class PointsController {

    @Autowired
    private PointsService pointsService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserPointsMapper userPointsMapper;

    @GetMapping("/my")
    public Result<Map<String, Object>> myPoints(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(pointsService.getUserPoints(userId));
    }

    @PostMapping("/checkin")
    public Result<Map<String, Object>> checkin(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(pointsService.dailyCheckin(userId));
    }

    /**
     * 获取用户资料 + 等级 + 积分
     */
    @GetMapping("/profile")
    public Result<Map<String, Object>> profile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var profile = userProfileService.getOrInitProfile(userId);
        // 获取积分信息
        UserPoints up = userPointsMapper.selectOne(
                new LambdaQueryWrapper<UserPoints>().eq(UserPoints::getUserId, userId));
        // 获取等级配置
        var levels = userProfileService.listLevelConfig();
        String levelName = "青铜";
        int levelCode = profile.getLevel() != null ? profile.getLevel() : 1;
        for (var lv : levels) {
            if (lv.getLevelCode() != null && lv.getLevelCode().equals(levelCode)) {
                levelName = lv.getLevelName();
                break;
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("profile", profile);
        data.put("levelName", levelName);
        data.put("totalPoints", up != null && up.getTotalPoints() != null ? up.getTotalPoints() : 0);
        data.put("experience", up != null && up.getExperience() != null ? up.getExperience() : 0);
        data.put("checkinStreak", up != null && up.getCheckinStreak() != null ? up.getCheckinStreak() : 0);
        return Result.success(data);
    }

    /**
     * 获取等级配置列表
     */
    @GetMapping("/level-config")
    public Result<?> levelConfig() {
        return Result.success(userProfileService.listLevelConfig());
    }

    /**
     * 分页查询积分明细
     */
    @GetMapping("/integral-logs")
    public Result<PageResult<?>> integralLogs(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var page = userProfileService.listIntegralLogs(userId, current, size);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(),
                page.getSize(), page.getCurrent()));
    }
}
