package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.QuestionTag;
import com.zhikao.entity.QuestionTagRel;
import com.zhikao.mapper.QuestionTagMapper;
import com.zhikao.mapper.QuestionTagRelMapper;
import com.zhikao.service.QuestionTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 题目标签Service实现
 */
@Service
public class QuestionTagServiceImpl extends ServiceImpl<QuestionTagMapper, QuestionTag> implements QuestionTagService {

    @Autowired
    private QuestionTagRelMapper questionTagRelMapper;

    @Override
    public List<QuestionTag> listTags(String keyword) {
        LambdaQueryWrapper<QuestionTag> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(QuestionTag::getTagName, keyword);
        }
        wrapper.orderByDesc(QuestionTag::getUsageCount);
        return list(wrapper);
    }

    @Override
    public List<QuestionTag> hotTags(int limit) {
        LambdaQueryWrapper<QuestionTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(QuestionTag::getUsageCount);
        wrapper.last("LIMIT " + Math.max(limit, 1));
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void tagQuestion(Long questionId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : tagIds) {
            // 重复忽略
            LambdaQueryWrapper<QuestionTagRel> relWrapper = new LambdaQueryWrapper<>();
            relWrapper.eq(QuestionTagRel::getQuestionId, questionId)
                      .eq(QuestionTagRel::getTagId, tagId);
            Long existCount = questionTagRelMapper.selectCount(relWrapper);
            if (existCount != null && existCount > 0) {
                continue;
            }
            // 插入关联
            QuestionTagRel rel = new QuestionTagRel();
            rel.setQuestionId(questionId);
            rel.setTagId(tagId);
            rel.setCreatedAt(new Date());
            questionTagRelMapper.insert(rel);

            // tag.usageCount+1
            QuestionTag tag = getById(tagId);
            if (tag != null) {
                tag.setUsageCount((tag.getUsageCount() == null ? 0 : tag.getUsageCount()) + 1);
                tag.setUpdatedAt(new Date());
                updateById(tag);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void untagQuestion(Long questionId, Long tagId) {
        LambdaQueryWrapper<QuestionTagRel> relWrapper = new LambdaQueryWrapper<>();
        relWrapper.eq(QuestionTagRel::getQuestionId, questionId)
                  .eq(QuestionTagRel::getTagId, tagId);
        int deleted = questionTagRelMapper.delete(relWrapper);
        if (deleted > 0) {
            // tag.usageCount-1
            QuestionTag tag = getById(tagId);
            if (tag != null && tag.getUsageCount() != null && tag.getUsageCount() > 0) {
                tag.setUsageCount(tag.getUsageCount() - 1);
                tag.setUpdatedAt(new Date());
                updateById(tag);
            }
        }
    }

    @Override
    public List<Long> listQuestionIdsByTag(Long tagId) {
        LambdaQueryWrapper<QuestionTagRel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionTagRel::getTagId, tagId);
        List<QuestionTagRel> rels = questionTagRelMapper.selectList(wrapper);
        if (rels.isEmpty()) {
            return new ArrayList<>();
        }
        return rels.stream().map(QuestionTagRel::getQuestionId).distinct().collect(Collectors.toList());
    }
}
