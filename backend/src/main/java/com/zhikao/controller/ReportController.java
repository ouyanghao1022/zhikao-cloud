package com.zhikao.controller;

import com.zhikao.common.Result;
import com.zhikao.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 学情报告控制器
 */
@RestController
@RequestMapping("/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 个人学情报告（含能力模型、趋势等综合数据）
     */
    @GetMapping("/personal")
    public Result<Map<String, Object>> personalReport(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(reportService.getPersonalReport(userId));
    }

    /**
     * 考试趋势（折线图数据）
     */
    @GetMapping("/exam-trend")
    public Result<Map<String, Object>> examTrend(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(reportService.getExamTrend(userId));
    }

    /**
     * 知识点掌握热力图
     */
    @GetMapping("/knowledge-heatmap")
    public Result<Map<String, Object>> knowledgeHeatmap(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(reportService.getKnowledgeHeatmap(userId));
    }

    /**
     * 薄弱知识点分析
     */
    @GetMapping("/weak-areas")
    public Result<Map<String, Object>> weakAreas(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(reportService.getWeakAreas(userId));
    }

    /**
     * 班级成绩分布（教师端，按班级过滤）
     */
    @PreAuthorize("hasAnyRole('TEACHER', 'SUPER_ADMIN')")
    @GetMapping("/class/{paperId}")
    public Result<Map<String, Object>> classReport(
            @PathVariable Long paperId,
            @RequestParam(required = false) Long classId) {
        return Result.success(reportService.getClassReport(paperId, classId));
    }
}
