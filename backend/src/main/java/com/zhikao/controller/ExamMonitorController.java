package com.zhikao.controller;

import com.zhikao.common.Result;
import com.zhikao.entity.ExamMonitorLog;
import com.zhikao.service.ExamMonitorLogService;
import com.zhikao.service.ExamSessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 考试监控控制器 — 防作弊事件上报与查询
 */
@RestController
@RequestMapping("/exam/monitor")
public class ExamMonitorController {

    @Autowired
    private ExamMonitorLogService examMonitorLogService;

    @Autowired
    private ExamSessionService examSessionService;

    /**
     * 学生端：上报防作弊事件（切屏/异常速度等）
     * body: {sessionId, eventType, detail, riskLevel}
     */
    @PostMapping("/event")
    public Result<?> reportEvent(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long sessionId = Long.valueOf(params.get("sessionId").toString());
        Integer eventType = (Integer) params.get("eventType");
        String detail = params.get("detail") != null ? params.get("detail").toString() : null;
        Integer riskLevel = params.get("riskLevel") != null
                ? Integer.valueOf(params.get("riskLevel").toString()) : 0;

        examMonitorLogService.recordEvent(sessionId, eventType, detail, riskLevel);

        // 切屏事件累加到 session.screenSwitchCount
        if (eventType != null && eventType == 1) {
            try {
                var session = examSessionService.getById(sessionId);
                if (session != null) {
                    session.setScreenSwitchCount(
                            (session.getScreenSwitchCount() != null ? session.getScreenSwitchCount() : 0) + 1);
                    // 风险等级高或切屏过多 → 标记疑似作弊
                    if (riskLevel != null && riskLevel >= 2) {
                        session.setCheatFlag(Math.max(session.getCheatFlag() != null ? session.getCheatFlag() : 0, 1));
                    }
                    examSessionService.updateById(session);
                }
            } catch (Exception ignored) {}
        }

        return Result.success("已记录");
    }

    /**
     * 管理端：查询某次考试记录的监控事件
     */
    @PreAuthorize("hasAnyRole('TEACHER', 'SUPER_ADMIN')")
    @GetMapping("/{sessionId}")
    public Result<List<ExamMonitorLog>> listBySession(@PathVariable Long sessionId) {
        return Result.success(examMonitorLogService.listBySession(sessionId));
    }
}
