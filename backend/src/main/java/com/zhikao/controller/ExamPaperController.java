package com.zhikao.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhikao.common.PageRequest;
import com.zhikao.common.PageResult;
import com.zhikao.common.Result;
import com.zhikao.entity.ExamPaper;
import com.zhikao.service.ExamPaperService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import java.util.List;
import java.util.Map;

/**
 * 试卷管理控制器
 */
@RestController
@RequestMapping("/exam")
public class ExamPaperController {

    @Autowired
    private ExamPaperService examPaperService;

    /**
     * 分页查询试卷列表
     * - 教师/管理员：显示全部（可按班级筛选）
     * - 学生：只显示发布到学生所在班级的考试
     */
    @GetMapping("/list")
    public Result<PageResult<ExamPaper>> list(PageRequest pageRequest,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(required = false) Long categoryId,
                                                @RequestParam(required = false) Long classId,
                                                HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        Long userId = (Long) request.getAttribute("userId");

        IPage<ExamPaper> page;
        if ("STUDENT".equals(role)) {
            // 学生端：只显示本班考试
            page = examPaperService.pageListForStudent(
                    pageRequest.getCurrent(), pageRequest.getSize(), userId);
        } else {
            // 管理员看全部，教师只看自己创建的试卷
            Long creatorId = "TEACHER".equals(role) ? userId : null;
            page = examPaperService.pageList(
                    pageRequest.getCurrent(), pageRequest.getSize(), status, categoryId, classId, creatorId);
        }
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(),
                page.getSize(), page.getCurrent()));
    }

    /**
     * 获取试卷详情
     */
    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(examPaperService.getPaperDetail(id));
    }

    /**
     * 创建试卷（教师/管理员）
     * paperType=1 固定组卷 → 需要 questionIds + questionScores
     * paperType=2 随机组卷 → 需要 randomConfig
     */
    @PreAuthorize("hasAnyRole('TEACHER', 'SUPER_ADMIN')")
    @PostMapping("/create")
    public Result<?> create(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long creatorId = (Long) request.getAttribute("userId");

        ExamPaper paper = new ExamPaper();
        paper.setTitle((String) params.get("title"));
        paper.setDescription((String) params.get("description"));
        paper.setDuration(params.get("duration") != null ? ((Number) params.get("duration")).intValue() : 60);
        paper.setTotalScore(params.get("totalScore") != null ?
                new java.math.BigDecimal(params.get("totalScore").toString()) : java.math.BigDecimal.valueOf(100));
        paper.setPassScore(params.get("passScore") != null ?
                new java.math.BigDecimal(params.get("passScore").toString()) : java.math.BigDecimal.valueOf(60));
        paper.setPaperType(params.get("paperType") != null ? ((Number) params.get("paperType")).intValue() : 1);
        if (params.containsKey("maxScreenSwitch") && params.get("maxScreenSwitch") != null) {
            paper.setMaxScreenSwitch(((Number) params.get("maxScreenSwitch")).intValue());
        }
        // 考试时间段
        if (params.containsKey("startTime") && params.get("startTime") != null) {
            paper.setStartTime(java.sql.Timestamp.valueOf(params.get("startTime").toString().replace("T", " ")));
        }
        if (params.containsKey("endTime") && params.get("endTime") != null) {
            paper.setEndTime(java.sql.Timestamp.valueOf(params.get("endTime").toString().replace("T", " ")));
        }
        // 考试时间为必填（错题解析门槛依赖结束时间）
        if (paper.getStartTime() == null || paper.getEndTime() == null) {
            return Result.badRequest("考试开始时间和结束时间为必填项");
        }
        paper.setCreatorId(creatorId);

        // 出题老师范围：admin 用传入的 teacherIds；教师自动填自己
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        List<Long> teacherIds;
        if (isAdmin) {
            @SuppressWarnings("unchecked")
            List<Object> rawTeacherIds = (List<Object>) params.get("teacherIds");
            teacherIds = rawTeacherIds == null ? java.util.List.of() :
                    rawTeacherIds.stream().map(o -> Long.valueOf(o.toString())).toList();
            if (teacherIds.isEmpty()) {
                return Result.badRequest("请选择至少一位出题老师");
            }
        } else {
            teacherIds = java.util.List.of(creatorId);
        }
        paper.setTeacherIds(String.join(",", teacherIds.stream().map(String::valueOf).toList()));

        @SuppressWarnings("unchecked")
        List<Long> questionIds = params.containsKey("questionIds") ?
                (List<Long>) params.get("questionIds") : null;
        @SuppressWarnings("unchecked")
        List<String> questionScores = params.containsKey("questionScores") ?
                (List<String>) params.get("questionScores") : null;

        @SuppressWarnings("unchecked")
        Map<String, Object> randomConfig = params.containsKey("randomConfig") ?
                (Map<String, Object>) params.get("randomConfig") : null;

        examPaperService.createPaper(paper, questionIds, questionScores, randomConfig);
        return Result.success("创建成功", paper.getId());
    }

    /**
     * 重新抽题（仅草稿随机试卷）
     */
    @PreAuthorize("hasAnyRole('TEACHER', 'SUPER_ADMIN')")
    @PostMapping("/{id}/reassemble")
    public Result<?> reassemble(@PathVariable Long id) {
        try {
            return Result.success(examPaperService.reassembleRandomPaper(id));
        } catch (RuntimeException e) {
            return Result.badRequest(e.getMessage());
        }
    }

    /**
     * 发布试卷
     */
    @PreAuthorize("hasAnyRole('TEACHER', 'SUPER_ADMIN')")
    @PutMapping("/publish/{id}")
    public Result<?> publish(@PathVariable Long id,
                              @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> classIds = body != null && body.containsKey("classIds")
                ? ((List<Number>) body.get("classIds")).stream().map(Number::longValue).toList()
                : List.of();
        examPaperService.publishPaper(id, classIds);
        return Result.success("发布成功");
    }

    /**
     * 删除试卷
     */
    @PreAuthorize("hasAnyRole('TEACHER', 'SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<?> deletePaper(@PathVariable Long id) {
        examPaperService.deletePaper(id);
        return Result.success("已删除");
    }

    /**
     * 学生参加考试 - 获取试卷题目
     */
    @GetMapping("/take/{id}")
    public Result<Map<String, Object>> takeExam(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(examPaperService.getExamForTaking(id, userId));
    }

    /**
     * 提交考试答案
     */
    @PostMapping("/submit")
    public Result<?> submitExam(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long paperId = Long.valueOf(params.get("paperId").toString());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) params.get("answers");

        Map<String, Object> result = examPaperService.submitExam(paperId, userId, answers);
        return Result.success("提交成功", result);
    }

    /**
     * 查询我的考试记录
     */
    @GetMapping("/my-records")
    public Result<PageResult<Map<String, Object>>> myRecords(PageRequest pageRequest,
                                                              HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var page = examPaperService.getMyRecords(userId, pageRequest.getCurrent(), pageRequest.getSize());
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(),
                page.getSize(), page.getCurrent()));
    }
}
