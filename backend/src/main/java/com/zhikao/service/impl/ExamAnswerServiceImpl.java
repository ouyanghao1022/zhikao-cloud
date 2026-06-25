package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.ExamAnswer;
import com.zhikao.entity.ExamSession;
import com.zhikao.entity.Question;
import com.zhikao.mapper.ExamAnswerMapper;
import com.zhikao.service.ExamAnswerService;
import com.zhikao.service.ExamSessionService;
import com.zhikao.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 答题记录Service实现
 */
@Service
public class ExamAnswerServiceImpl extends ServiceImpl<ExamAnswerMapper, ExamAnswer> implements ExamAnswerService {

    /** 主观题题型：5简答 6论述 7编程 8组合 */
    private static final List<Integer> SUBJECTIVE_TYPES = Arrays.asList(5, 6, 7, 8);

    @Autowired
    private ExamSessionService examSessionService;

    @Autowired
    private QuestionService questionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void gradeAnswer(Long answerId, Long teacherId, BigDecimal score, String feedback) {
        ExamAnswer answer = getById(answerId);
        if (answer == null) {
            throw new RuntimeException("答题记录不存在");
        }
        if (score == null) score = BigDecimal.ZERO;
        answer.setScore(score);
        answer.setCorrectedBy(teacherId);
        answer.setCorrectedAt(new Date());
        answer.setIsCorrect(score.compareTo(BigDecimal.ZERO) > 0 ? 1 : 0);
        updateById(answer);

        // 重算所属 session 的分数与状态
        recalcSessionScores(answer.getSessionId());
    }

    /**
     * 重算某次考试记录的总分/客观分/主观分/批阅状态
     */
    private void recalcSessionScores(Long sessionId) {
        List<ExamAnswer> all = list(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getSessionId, sessionId));
        BigDecimal objective = BigDecimal.ZERO;
        BigDecimal subjective = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        boolean allSubjectiveGraded = true;

        for (ExamAnswer a : all) {
            BigDecimal s = a.getScore() != null ? a.getScore() : BigDecimal.ZERO;
            total = total.add(s);
            Question q = questionService.getById(a.getQuestionId());
            if (q != null && SUBJECTIVE_TYPES.contains(q.getQuestionType())) {
                subjective = subjective.add(s);
                if (a.getCorrectedBy() == null) {
                    allSubjectiveGraded = false;
                }
            } else {
                objective = objective.add(s);
            }
        }

        ExamSession session = examSessionService.getById(sessionId);
        if (session != null) {
            session.setObjectiveScore(objective);
            session.setSubjectiveScore(subjective);
            session.setTotalScore(total);
            // 所有主观题都已批阅 → 已批阅；否则仍待阅
            session.setStatus(allSubjectiveGraded ? 3 : 2);
            session.setUpdatedAt(new Date());
            examSessionService.updateById(session);
        }
    }

    @Override
    public List<Map<String, Object>> listPendingForGrade(Long paperId) {
        // 查该试卷下状态为"待阅"(2)的考试记录
        List<ExamSession> sessions = examSessionService.list(
                new LambdaQueryWrapper<ExamSession>()
                        .eq(ExamSession::getPaperId, paperId)
                        .eq(ExamSession::getStatus, 2));
        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamSession s : sessions) {
            List<Map<String, Object>> subjectiveAnswers = new ArrayList<>();
            List<ExamAnswer> answers = list(new LambdaQueryWrapper<ExamAnswer>()
                    .eq(ExamAnswer::getSessionId, s.getId()));
            for (ExamAnswer a : answers) {
                Question q = questionService.getById(a.getQuestionId());
                if (q != null && SUBJECTIVE_TYPES.contains(q.getQuestionType())) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("answerId", a.getId());
                    m.put("questionId", q.getId());
                    m.put("title", q.getTitle());
                    m.put("questionType", q.getQuestionType());
                    m.put("referenceAnswer", q.getAnswer());
                    m.put("userAnswer", a.getUserAnswer());
                    m.put("currentScore", a.getScore());
                    m.put("graded", a.getCorrectedBy() != null);
                    subjectiveAnswers.add(m);
                }
            }
            if (!subjectiveAnswers.isEmpty()) {
                Map<String, Object> sm = new HashMap<>();
                sm.put("sessionId", s.getId());
                sm.put("userId", s.getUserId());
                sm.put("studentName", s.getUserId());
                sm.put("totalScore", s.getTotalScore());
                sm.put("answers", subjectiveAnswers);
                result.add(sm);
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> listBySessionWithQuestion(Long sessionId) {
        List<ExamAnswer> answers = list(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getSessionId, sessionId)
                .orderByAsc(ExamAnswer::getQuestionId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamAnswer a : answers) {
            Map<String, Object> m = new HashMap<>();
            Question q = questionService.getById(a.getQuestionId());
            m.put("answerId", a.getId());
            m.put("questionId", a.getQuestionId());
            m.put("title", q != null ? q.getTitle() : null);
            m.put("questionType", q != null ? q.getQuestionType() : null);
            m.put("referenceAnswer", q != null ? q.getAnswer() : null);
            m.put("answerAnalysis", q != null ? q.getAnswerAnalysis() : null);
            m.put("userAnswer", a.getUserAnswer());
            m.put("score", a.getScore());
            m.put("isCorrect", a.getIsCorrect());
            m.put("graded", a.getCorrectedBy() != null);
            m.put("isSubjective", q != null && SUBJECTIVE_TYPES.contains(q.getQuestionType()));
            result.add(m);
        }
        return result;
    }
}
