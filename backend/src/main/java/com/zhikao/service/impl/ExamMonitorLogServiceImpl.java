package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.ExamMonitorLog;
import com.zhikao.mapper.ExamMonitorLogMapper;
import com.zhikao.service.ExamMonitorLogService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 考试监控日志Service实现
 */
@Service
public class ExamMonitorLogServiceImpl extends ServiceImpl<ExamMonitorLogMapper, ExamMonitorLog>
        implements ExamMonitorLogService {

    @Override
    public void recordEvent(Long sessionId, Integer eventType, String eventDetail, Integer riskLevel) {
        if (sessionId == null || eventType == null) return;
        try {
            ExamMonitorLog log = new ExamMonitorLog();
            log.setSessionId(sessionId);
            log.setEventType(eventType);
            log.setEventDetail(eventDetail);
            log.setRiskLevel(riskLevel != null ? riskLevel : 0);
            log.setCreatedAt(new Date());
            save(log);
        } catch (Exception ignored) {
        }
    }

    @Override
    public List<ExamMonitorLog> listBySession(Long sessionId) {
        LambdaQueryWrapper<ExamMonitorLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamMonitorLog::getSessionId, sessionId)
               .orderByAsc(ExamMonitorLog::getCreatedAt);
        return list(wrapper);
    }
}
