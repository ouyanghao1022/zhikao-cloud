package com.zhikao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.entity.QuestionTag;

import java.util.List;

/**
 * 题目标签Service
 */
public interface QuestionTagService extends IService<QuestionTag> {

    /**
     * 标签列表（可选关键字过滤）
     */
    List<QuestionTag> listTags(String keyword);

    /**
     * 热门标签（usageCount 降序）
     */
    List<QuestionTag> hotTags(int limit);

    /**
     * 给题目打标签（重复忽略，tag.usageCount+1）
     */
    void tagQuestion(Long questionId, List<Long> tagIds);

    /**
     * 取消题目标签（tag.usageCount-1）
     */
    void untagQuestion(Long questionId, Long tagId);

    /**
     * 按标签查题目列表
     */
    List<Long> listQuestionIdsByTag(Long tagId);
}
