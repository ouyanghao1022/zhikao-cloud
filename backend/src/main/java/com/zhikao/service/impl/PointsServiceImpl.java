package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.UserPoints;
import com.zhikao.mapper.UserPointsMapper;
import com.zhikao.service.PointsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class PointsServiceImpl extends ServiceImpl<UserPointsMapper, UserPoints> implements PointsService {

    private static final int[] LEVEL_THRESHOLDS = {0, 100, 300, 800, 2000, 5000, 12000};
    private static final String[] LEVEL_NAMES = {"青铜", "白银", "黄金", "铂金", "钻石", "星耀", "王者"};

    @Override
    public Map<String, Object> getUserPoints(Long userId) {
        UserPoints up = getOrCreate(userId);
        LocalDate today = LocalDate.now();
        LocalDate lastCheckin = up.getLastCheckinDate() != null
            ? new java.sql.Date(up.getLastCheckinDate().getTime()).toLocalDate()
            : null;
        boolean checkedToday = lastCheckin != null && lastCheckin.equals(today);

        Map<String, Object> data = new HashMap<>();
        data.put("totalPoints", up.getTotalPoints());
        data.put("level", up.getCurrentLevel());
        data.put("levelName", LEVEL_NAMES[up.getCurrentLevel() - 1]);
        data.put("experience", up.getExperience());
        data.put("nextLevelExp", LEVEL_THRESHOLDS[Math.min(up.getCurrentLevel(), 6)]);
        data.put("streak", up.getCheckinStreak());
        data.put("checkedToday", checkedToday);
        return data;
    }

    @Override
    @Transactional
    public Map<String, Object> dailyCheckin(Long userId) {
        UserPoints up = getOrCreate(userId);
        LocalDate today = LocalDate.now();
        LocalDate last = up.getLastCheckinDate() != null
            ? new java.sql.Date(up.getLastCheckinDate().getTime()).toLocalDate()
            : null;

        Map<String, Object> result = new HashMap<>();
        if (last != null && last.equals(today)) {
            result.put("success", false);
            result.put("message", "今天已签到");
            return result;
        }

        if (last != null && last.plusDays(1).equals(today)) {
            up.setCheckinStreak(up.getCheckinStreak() + 1);
        } else {
            up.setCheckinStreak(1);
        }

        int bonus = 5 + Math.min(up.getCheckinStreak(), 30);
        up.setTotalPoints(up.getTotalPoints() + bonus);
        up.setLastCheckinDate(new Date());
        up.setCurrentLevel(calculateLevel(up.getTotalPoints()));
        int nextThreshold = LEVEL_THRESHOLDS[Math.min(up.getCurrentLevel(), 6)];
        up.setExperience(nextThreshold > 0 ? up.getTotalPoints() % nextThreshold : 0);
        updateById(up);

        result.put("success", true);
        result.put("points", bonus);
        result.put("totalPoints", up.getTotalPoints());
        result.put("streak", up.getCheckinStreak());
        result.put("level", up.getCurrentLevel());
        result.put("levelName", LEVEL_NAMES[up.getCurrentLevel() - 1]);
        return result;
    }

    @Override
    @Transactional
    public void addPoints(Long userId, int points, String reason) {
        UserPoints up = getOrCreate(userId);
        up.setTotalPoints(up.getTotalPoints() + points);
        up.setCurrentLevel(calculateLevel(up.getTotalPoints()));
        int nextThreshold = LEVEL_THRESHOLDS[Math.min(up.getCurrentLevel(), 6)];
        up.setExperience(nextThreshold > 0 ? up.getTotalPoints() % nextThreshold : 0);
        updateById(up);
    }

    @Override
    public int calculateLevel(int totalPoints) {
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (totalPoints >= LEVEL_THRESHOLDS[i]) return i + 1;
        }
        return 1;
    }

    private UserPoints getOrCreate(Long userId) {
        UserPoints up = getOne(new LambdaQueryWrapper<UserPoints>().eq(UserPoints::getUserId, userId));
        if (up == null) {
            up = new UserPoints();
            up.setUserId(userId);
            up.setTotalPoints(0);
            up.setCurrentLevel(1);
            up.setExperience(0);
            up.setCheckinStreak(0);
            up.setCreatedAt(new Date());
            up.setUpdatedAt(new Date());
            save(up);
        }
        return up;
    }
}
