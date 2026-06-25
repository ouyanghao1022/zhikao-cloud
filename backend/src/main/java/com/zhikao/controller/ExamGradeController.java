package com.zhikao.controller;

import com.zhikao.common.Result;
import com.zhikao.service.ExamAnswerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 主观题批阅控制器（教师/管理员）
 */
@RestController
@RequestMapping("/exam/grade")
@PreAuthorize("hasAnyRole('TEACHER', 'SUPER_ADMIN')")
public class ExamGradeController {

    @Autowired
    private ExamAnswerService examAnswerService;

    /**
     * 批阅单题（覆盖 AI 预评分）
     * body: {answerId, score, feedback?}
     */
    @PostMapping("/answer")
    public Result<?> gradeAnswer(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long answerId = Long.valueOf(params.get("answerId").toString());
        BigDecimal score = new BigDecimal(params.get("score").toString());
        String feedback = params.get("feedback") != null ? params.get("feedback").toString() : null;
        Long teacherId = (Long) request.getAttribute("userId");
        examAnswerService.gradeAnswer(answerId, teacherId, score, feedback);
        return Result.success("批阅成功");
    }

    /**
     * 查询某试卷下待批阅的主观题列表
     */
    @GetMapping("/pending")
    public Result<List<Map<String, Object>>> pending(@RequestParam Long paperId) {
        return Result.success(examAnswerService.listPendingForGrade(paperId));
    }

    /**
     * 查询某次考试记录的答题详情（批阅页用）
     */
    @GetMapping("/session/{sessionId}")
    public Result<List<Map<String, Object>>> sessionDetail(@PathVariable Long sessionId) {
        return Result.success(examAnswerService.listBySessionWithQuestion(sessionId));
    }
}
