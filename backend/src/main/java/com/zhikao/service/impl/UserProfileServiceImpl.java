package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhikao.entity.*;
import com.zhikao.mapper.*;
import com.zhikao.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/**
 * 用户扩展信息Service实现类
 */
@Service
public class UserProfileServiceImpl implements UserProfileService {

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private UserPointsMapper userPointsMapper;

    @Autowired
    private UserIntegralLogMapper userIntegralLogMapper;

    @Autowired
    private UserLevelMapper userLevelMapper;

    @Autowired
    private ExamSessionMapper examSessionMapper;

    @Autowired
    private PkMatchRecordMapper pkMatchRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfile getOrInitProfile(Long userId) {
        LambdaQueryWrapper<UserProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProfile::getUserId, userId);
        UserProfile profile = userProfileMapper.selectOne(wrapper);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setTotalExams(0);
            profile.setAvgScore(BigDecimal.ZERO);
            profile.setWinRate(BigDecimal.ZERO);
            profile.setTotalBadges(0);
            profile.setLevel(1);
            profile.setExperience(0);
            profile.setIntegration(0);
            profile.setContinuousSignDays(0);
            profile.setCreatedAt(new Date());
            profile.setUpdatedAt(new Date());
            userProfileMapper.insert(profile);
        }
        return profile;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recalcStats(Long userId) {
        UserProfile profile = getOrInitProfile(userId);

        // 从 exam_session 聚合：已提交(status>=1)且有答题记录的考试
        LambdaQueryWrapper<ExamSession> examWrapper = new LambdaQueryWrapper<>();
        examWrapper.eq(ExamSession::getUserId, userId)
                   .in(ExamSession::getStatus, 1, 2, 3)
                   .exists("SELECT 1 FROM exam_answer ea WHERE ea.session_id = exam_session.id");
        List<ExamSession> sessions = examSessionMapper.selectList(examWrapper);
        int totalExams = sessions.size();
        BigDecimal avgScore = BigDecimal.ZERO;
        if (totalExams > 0) {
            BigDecimal sum = BigDecimal.ZERO;
            int valid = 0;
            for (ExamSession s : sessions) {
                if (s.getTotalScore() != null) {
                    sum = sum.add(s.getTotalScore());
                    valid++;
                }
            }
            if (valid > 0) {
                avgScore = sum.divide(BigDecimal.valueOf(valid), 2, RoundingMode.HALF_UP);
            }
        }
        profile.setTotalExams(totalExams);
        profile.setAvgScore(avgScore);

        // 从 pk_match_record 聚合：答题正确率作为胜率近似
        LambdaQueryWrapper<PkMatchRecord> pkWrapper = new LambdaQueryWrapper<>();
        pkWrapper.eq(PkMatchRecord::getUserId, userId);
        long totalAnswers = pkMatchRecordMapper.selectCount(pkWrapper);
        BigDecimal winRate = BigDecimal.ZERO;
        if (totalAnswers > 0) {
            LambdaQueryWrapper<PkMatchRecord> correctWrapper = new LambdaQueryWrapper<>();
            correctWrapper.eq(PkMatchRecord::getUserId, userId)
                          .eq(PkMatchRecord::getIsCorrect, 1);
            long correct = pkMatchRecordMapper.selectCount(correctWrapper);
            winRate = BigDecimal.valueOf(correct)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalAnswers), 2, RoundingMode.HALF_UP);
        }
        profile.setWinRate(winRate);

        profile.setUpdatedAt(new Date());
        userProfileMapper.updateById(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordIntegral(Long userId, Integer changeType, Integer value, String desc, Long relatedId) {
        if (value == null) value = 0;
        // 读取当前积分
        LambdaQueryWrapper<UserPoints> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPoints::getUserId, userId);
        UserPoints up = userPointsMapper.selectOne(wrapper);
        int currentPoints = up != null && up.getTotalPoints() != null ? up.getTotalPoints() : 0;
        int newValue = currentPoints + value;

        // 写积分日志（currentValue=变动后）
        UserIntegralLog log = new UserIntegralLog();
        log.setUserId(userId);
        log.setChangeType(changeType);
        log.setChangeValue(value);
        log.setCurrentValue(newValue);
        log.setDescription(desc);
        log.setRelatedId(relatedId);
        log.setCreatedAt(new Date());
        userIntegralLogMapper.insert(log);

        // 更新 user_points.total_points
        if (up != null) {
            up.setTotalPoints(newValue);
            up.setUpdatedAt(new Date());
            userPointsMapper.updateById(up);
        } else {
            up = new UserPoints();
            up.setUserId(userId);
            up.setTotalPoints(newValue);
            up.setCurrentLevel(1);
            up.setExperience(0);
            up.setCheckinStreak(0);
            up.setCreatedAt(new Date());
            up.setUpdatedAt(new Date());
            userPointsMapper.insert(up);
        }
    }

    @Override
    public List<UserLevel> listLevelConfig() {
        LambdaQueryWrapper<UserLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(UserLevel::getLevelCode);
        return userLevelMapper.selectList(wrapper);
    }

    @Override
    public IPage<UserIntegralLog> listIntegralLogs(Long userId, long current, long size) {
        Page<UserIntegralLog> page = new Page<>(current, size);
        LambdaQueryWrapper<UserIntegralLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserIntegralLog::getUserId, userId)
               .orderByDesc(UserIntegralLog::getCreatedAt);
        return userIntegralLogMapper.selectPage(page, wrapper);
    }
}
