package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.Question;
import com.zhikao.mapper.QuestionMapper;
import com.zhikao.service.QuestionService;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 题库Service实现类
 */
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Override
    public List<Question> randomPaper(Long categoryId, Integer totalCount,
                                      Map<Integer, Integer> typeCountMap,
                                      Map<Integer, Integer> difficultyPercentMap,
                                      List<Long> creatorIds) {
        List<Question> result = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : typeCountMap.entrySet()) {
            Integer questionType = entry.getKey();
            Integer count = entry.getValue();

            LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Question::getQuestionType, questionType)
                    .eq(Question::getStatus, 1);
            if (categoryId != null && categoryId > 0) {
                wrapper.eq(Question::getCategoryId, categoryId);
            }
            if (creatorIds != null && !creatorIds.isEmpty()) {
                wrapper.in(Question::getCreatorId, creatorIds);
            }
            // 按难度比例过滤
            Integer percent = difficultyPercentMap != null ? difficultyPercentMap.get(questionType) : null;
            if (percent != null) {
                // 根据百分比随机分配难度
                int[] difficulties = getDifficultiesByPercent(percent);
                wrapper.in(Question::getDifficulty, difficulties);
            }

            List<Question> questions = baseMapper.selectList(wrapper);
            // 随机打乱并取前count道
            Collections.shuffle(questions);
            if (questions.size() >= count) {
                result.addAll(questions.subList(0, count));
            } else {
                result.addAll(questions);
            }
        }
        return result;
    }

    @Override
    public com.baomidou.mybatisplus.core.metadata.IPage<Question> pageList(long current, long size,
                                                                           Long categoryId, Integer questionType,
                                                                           Integer difficulty, String keyword,
                                                                           List<Long> creatorIds) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Question::getStatus, 1);
        if (categoryId != null && categoryId > 0) {
            wrapper.eq(Question::getCategoryId, categoryId);
        }
        if (questionType != null && questionType > 0) {
            wrapper.eq(Question::getQuestionType, questionType);
        }
        if (difficulty != null && difficulty > 0) {
            wrapper.eq(Question::getDifficulty, difficulty);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Question::getTitle, keyword);
        }
        if (creatorIds != null && !creatorIds.isEmpty()) {
            wrapper.in(Question::getCreatorId, creatorIds);
        }
        wrapper.orderByDesc(Question::getCreatedAt);
        return page(new Page<>(current, size), wrapper);
    }

    private int[] getDifficultiesByPercent(Integer percent) {
        if (percent <= 30) return new int[]{1};
        if (percent <= 80) return new int[]{1, 2};
        return new int[]{1, 2, 3};
    }
}
