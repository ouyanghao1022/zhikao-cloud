package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.QuestionAnalysis;
import com.zhikao.mapper.QuestionAnalysisMapper;
import com.zhikao.service.QuestionAnalysisService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 题目解析Service实现
 */
@Service
public class QuestionAnalysisServiceImpl extends ServiceImpl<QuestionAnalysisMapper, QuestionAnalysis> implements QuestionAnalysisService {

    @Override
    public QuestionAnalysis getByQuestionId(Long questionId) {
        LambdaQueryWrapper<QuestionAnalysis> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionAnalysis::getQuestionId, questionId);
        return getOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionAnalysis saveOrUpdateByQuestionId(QuestionAnalysis analysis) {
        if (analysis.getQuestionId() == null) {
            throw new IllegalArgumentException("questionId 不能为空");
        }
        LambdaQueryWrapper<QuestionAnalysis> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionAnalysis::getQuestionId, analysis.getQuestionId());
        QuestionAnalysis existing = getOne(wrapper);
        if (existing != null) {
            // upsert：更新已有记录
            existing.setTextAnalysis(analysis.getTextAnalysis());
            existing.setVideoUrl(analysis.getVideoUrl());
            existing.setKnowledgePoints(analysis.getKnowledgePoints());
            existing.setUpdatedAt(new Date());
            updateById(existing);
            return existing;
        }
        // 新增
        analysis.setCreatedAt(new Date());
        analysis.setUpdatedAt(new Date());
        save(analysis);
        return analysis;
    }
}
