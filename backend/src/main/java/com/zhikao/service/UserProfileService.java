package com.zhikao.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhikao.entity.UserIntegralLog;
import com.zhikao.entity.UserLevel;
import com.zhikao.entity.UserProfile;

import java.util.List;

/**
 * 用户扩展信息Service接口（资料/统计/积分日志/等级配置）
 */
public interface UserProfileService {

    /**
     * 获取用户资料，无则初始化（level=1, 各统计0）
     */
    UserProfile getOrInitProfile(Long userId);

    /**
     * 从 exam_session 聚合 totalExams/avgScore、从 pk 聚合 winRate，更新 user_profile
     */
    void recalcStats(Long userId);

    /**
     * 记录积分变动：读 user_points 当前积分，写 user_integral_log（currentValue=变动后），
     * 并更新 user_points.total_points
     */
    void recordIntegral(Long userId, Integer changeType, Integer value, String desc, Long relatedId);

    /**
     * 查询全部等级配置，按 levelCode 升序
     */
    List<UserLevel> listLevelConfig();

    /**
     * 分页查询用户积分明细
     */
    IPage<UserIntegralLog> listIntegralLogs(Long userId, long current, long size);
}
