package com.zhikao.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhikao.common.PageRequest;
import com.zhikao.common.PageResult;
import com.zhikao.common.Result;
import com.zhikao.entity.Question;
import com.zhikao.entity.WrongNotebook;
import com.zhikao.service.QuestionService;
import com.zhikao.service.WrongNotebookService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 错题本控制器
 */
@RestController
@RequestMapping("/wrong-notebook")
public class WrongNotebookController {

    @Autowired
    private WrongNotebookService wrongNotebookService;

    @Autowired
    private QuestionService questionService;

    /**
     * 分页查询我的错题本
     */
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(PageRequest pageRequest,
                                                         @RequestParam(required = false) Integer masterStatus,
                                                         @RequestParam(required = false) Long categoryId,
                                                         HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        LambdaQueryWrapper<WrongNotebook> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WrongNotebook::getUserId, userId);
        if (masterStatus != null) {
            wrapper.eq(WrongNotebook::getMasterStatus, masterStatus);
        }
        wrapper.orderByDesc(WrongNotebook::getCreatedAt);

        Page<WrongNotebook> page = wrongNotebookService.page(
                new Page<>(pageRequest.getCurrent(), pageRequest.getSize()), wrapper);

        // 批量查题目信息
        List<Long> qids = page.getRecords().stream()
                .map(WrongNotebook::getQuestionId).distinct().toList();
        Map<Long, Question> qmap = Collections.emptyMap();
        if (!qids.isEmpty()) {
            qmap = questionService.listByIds(qids).stream()
                    .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));
        }

        // 组装返回
        List<Map<String, Object>> records = new ArrayList<>();
        for (WrongNotebook wn : page.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", wn.getId());
            item.put("userId", wn.getUserId());
            item.put("questionId", wn.getQuestionId());
            item.put("examSessionId", wn.getExamSessionId());
            item.put("wrongAnswer", wn.getWrongAnswer());
            item.put("correctAnswer", wn.getCorrectAnswer());
            item.put("errorType", wn.getErrorType());
            item.put("masterStatus", wn.getMasterStatus());
            item.put("reviewCount", wn.getReviewCount());
            item.put("lastReviewTime", wn.getLastReviewTime());
            item.put("nextReviewTime", wn.getNextReviewTime());
            item.put("createdAt", wn.getCreatedAt());
            item.put("updatedAt", wn.getUpdatedAt());

            Question q = qmap.get(wn.getQuestionId());
            if (q != null) {
                item.put("title", q.getTitle());
                item.put("questionType", q.getQuestionType());
                item.put("difficulty", q.getDifficulty());
            }
            if (categoryId != null && (q == null || !categoryId.equals(q.getCategoryId()))) {
                continue; // 按分类过滤
            }
            records.add(item);
        }

        return Result.success(PageResult.of(records, page.getTotal(),
                page.getSize(), page.getCurrent()));
    }

    /**
     * 更新掌握状态
     */
    @PutMapping("/master/{id}")
    public Result<?> updateMaster(@PathVariable Long id,
                                   @RequestBody Map<String, Integer> body) {
        WrongNotebook wn = wrongNotebookService.getById(id);
        if (wn == null) {
            return Result.error("记录不存在");
        }
        wn.setMasterStatus(body.getOrDefault("masterStatus", 0));
        wn.setUpdatedAt(new Date());
        wrongNotebookService.updateById(wn);
        return Result.success("更新成功");
    }

    /**
     * 删除错题记录
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        wrongNotebookService.removeById(id);
        return Result.success("已删除");
    }

    // ==================== 错题本扩展：复习计划/标签 ====================

    /**
     * 今日复习计划
     */
    @GetMapping("/review/today")
    public Result<Map<String, Object>> todayPlan(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(wrongNotebookService.getTodayPlan(userId));
    }

    /**
     * 标记复习完成
     */
    @PostMapping("/review/{noteId}/done")
    public Result<?> markReviewed(@PathVariable Long noteId) {
        try {
            wrongNotebookService.markReviewed(noteId);
            return Result.success("已标记复习完成");
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        }
    }

    /**
     * 给错题打标签（body:{tags:[]}）
     */
    @PostMapping("/{noteId}/tags")
    public Result<?> addTags(@PathVariable Long noteId, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) body.getOrDefault("tags", List.of());
        wrongNotebookService.tagWrongNote(noteId, tags);
        return Result.success("打标签成功");
    }

    /**
     * 查错题标签
     */
    @GetMapping("/{noteId}/tags")
    public Result<List<String>> listTags(@PathVariable Long noteId) {
        return Result.success(wrongNotebookService.listWrongNoteTags(noteId));
    }
}
