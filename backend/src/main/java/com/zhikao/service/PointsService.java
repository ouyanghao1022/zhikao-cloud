package com.zhikao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.entity.UserPoints;
import java.util.Map;

public interface PointsService extends IService<UserPoints> {
    /** 获取用户积分与等级 */
    Map<String, Object> getUserPoints(Long userId);
    /** 每日签到 */
    Map<String, Object> dailyCheckin(Long userId);
    /** 增加积分（考试/分享等） */
    void addPoints(Long userId, int points, String reason);
    /** 计算等级 (1青铜-2白银-3黄金-4铂金-5钻石-6星耀-7王者) */
    int calculateLevel(int totalPoints);
}
