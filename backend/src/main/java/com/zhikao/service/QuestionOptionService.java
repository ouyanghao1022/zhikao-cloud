package com.zhikao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.entity.QuestionOption;

import java.util.List;

/**
 * 题目选项Service
 */
public interface QuestionOptionService extends IService<QuestionOption> {

    /**
     * 根据题目ID查询选项列表
     */
    List<QuestionOption> selectByQuestionId(Long questionId);
}
