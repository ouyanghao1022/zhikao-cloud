package com.zhikao.service;

import com.zhikao.entity.ExamMonitorLog;
import java.util.List;

/**
 * 考试监控日志Service
 */
public interface ExamMonitorLogService {

    /** 记录监控事件 */
    void recordEvent(Long sessionId, Integer eventType, String eventDetail, Integer riskLevel);

    /** 按考试记录查询监控事件 */
    List<ExamMonitorLog> listBySession(Long sessionId);
}
