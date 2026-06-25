package com.zhikao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.entity.QuestionAnalysis;

/**
 * 题目解析Service
 */
public interface QuestionAnalysisService extends IService<QuestionAnalysis> {

    /**
     * 按题目ID获取解析（无则返回null）
     */
    QuestionAnalysis getByQuestionId(Long questionId);

    /**
     * 保存/更新解析（upsert，按 questionId 唯一）
     */
    QuestionAnalysis saveOrUpdateByQuestionId(QuestionAnalysis analysis);
}
