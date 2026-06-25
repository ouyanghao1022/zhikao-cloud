package com.zhikao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.QuestionOption;
import com.zhikao.mapper.QuestionOptionMapper;
import com.zhikao.service.QuestionOptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 题目选项Service实现
 */
@Service
public class QuestionOptionServiceImpl extends ServiceImpl<QuestionOptionMapper, QuestionOption> implements QuestionOptionService {

    @Autowired
    private QuestionOptionMapper questionOptionMapper;

    @Override
    public List<QuestionOption> selectByQuestionId(Long questionId) {
        return questionOptionMapper.selectByQuestionId(questionId);
    }
}
