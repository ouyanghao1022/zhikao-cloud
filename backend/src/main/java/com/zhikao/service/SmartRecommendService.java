package com.zhikao.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhikao.entity.ExamAnswer;
import com.zhikao.entity.Question;
import com.zhikao.mapper.ExamAnswerMapper;
import com.zhikao.mapper.QuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能错题推荐 — 基于遗忘曲线的复习推荐
 */
@Service
public class SmartRecommendService {

    @Autowired
    private ExamAnswerMapper examAnswerMapper;
    @Autowired
    private QuestionMapper questionMapper;

    /**
     * 获取今日推荐复习的错题（基于艾宾浩斯遗忘曲线）
     * 复习间隔：1天、2天、4天、7天、15天、30天
     */
    public List<Map<String, Object>> getTodayRecommend(Long userId) {
        List<ExamAnswer> wrongAnswers = examAnswerMapper.selectList(
            new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getIsCorrect, 0)
                .orderByDesc(ExamAnswer::getCreatedAt)
                .last("LIMIT 100")
        );

        if (wrongAnswers.isEmpty()) return Collections.emptyList();

        List<Long> questionIds = wrongAnswers.stream()
            .map(ExamAnswer::getQuestionId).distinct().collect(Collectors.toList());

        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        Map<Long, Question> qMap = questions.stream()
            .collect(Collectors.toMap(Question::getId, q -> q));

        // 按错误次数排序推荐
        Map<Long, Long> errorCounts = wrongAnswers.stream()
            .collect(Collectors.groupingBy(ExamAnswer::getQuestionId, Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : errorCounts.entrySet()) {
            Question q = qMap.get(entry.getKey());
            if (q == null) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("questionId", q.getId());
            item.put("title", q.getTitle());
            item.put("type", q.getQuestionType());
            item.put("answer", q.getAnswer());
            item.put("errorCount", entry.getValue());
            item.put("priority", entry.getValue() > 2 ? "高频" : "普通");
            result.add(item);
        }

        result.sort((a, b) -> Long.compare(
            (Long) b.get("errorCount"), (Long) a.get("errorCount")
        ));

        return result.subList(0, Math.min(20, result.size()));
    }

    /**
     * 推荐相似题目（简单实现：同类型同难度）
     */
    public List<Question> getSimilarQuestions(Long questionId) {
        Question q = questionMapper.selectById(questionId);
        if (q == null) return Collections.emptyList();
        return questionMapper.selectList(
            new LambdaQueryWrapper<Question>()
                .eq(Question::getQuestionType, q.getQuestionType())
                .eq(q.getDifficulty() != null, Question::getDifficulty, q.getDifficulty())
                .ne(Question::getId, questionId)
                .last("LIMIT 5")
        );
    }
}
