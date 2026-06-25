package com.zhikao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.entity.ExamAnswer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 答题记录Service
 */
public interface ExamAnswerService extends IService<ExamAnswer> {

    /** 教师批阅单题（覆盖AI预评分），并重算session总分 */
    void gradeAnswer(Long answerId, Long teacherId, BigDecimal score, String feedback);

    /** 查询某试卷下待批阅的主观题（session状态=待阅） */
    List<Map<String, Object>> listPendingForGrade(Long paperId);

    /** 查询某次考试记录的答题详情（含题干/参考答案/用户答案/分数） */
    List<Map<String, Object>> listBySessionWithQuestion(Long sessionId);
}
