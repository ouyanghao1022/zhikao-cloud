package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.Question;
import com.zhikao.entity.QuestionFavorite;
import com.zhikao.mapper.QuestionFavoriteMapper;
import com.zhikao.mapper.QuestionMapper;
import com.zhikao.service.QuestionFavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 题目收藏Service实现
 */
@Service
public class QuestionFavoriteServiceImpl extends ServiceImpl<QuestionFavoriteMapper, QuestionFavorite> implements QuestionFavoriteService {

    @Autowired
    private QuestionMapper questionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionFavorite favorite(Long userId, Long questionId, String note) {
        LambdaQueryWrapper<QuestionFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionFavorite::getUserId, userId)
               .eq(QuestionFavorite::getQuestionId, questionId);
        QuestionFavorite existing = getOne(wrapper);
        if (existing != null) {
            // 已存在则更新 note
            existing.setNote(note);
            baseMapper.updateById(existing);
            return existing;
        }
        QuestionFavorite fav = new QuestionFavorite();
        fav.setUserId(userId);
        fav.setQuestionId(questionId);
        fav.setNote(note);
        fav.setCreatedAt(new Date());
        save(fav);
        return fav;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfavorite(Long userId, Long questionId) {
        LambdaQueryWrapper<QuestionFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionFavorite::getUserId, userId)
               .eq(QuestionFavorite::getQuestionId, questionId);
        remove(wrapper);
    }

    @Override
    public IPage<QuestionFavorite> listMyFavorites(Long userId, long current, long size) {
        LambdaQueryWrapper<QuestionFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionFavorite::getUserId, userId)
               .orderByDesc(QuestionFavorite::getCreatedAt);
        Page<QuestionFavorite> page = new Page<>(current, size);
        IPage<QuestionFavorite> result = baseMapper.selectPage(page, wrapper);

        // 批量填充题目信息
        List<QuestionFavorite> records = result.getRecords();
        if (!records.isEmpty()) {
            Set<Long> qids = records.stream()
                    .map(QuestionFavorite::getQuestionId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());
            if (!qids.isEmpty()) {
                List<Question> questions = questionMapper.selectBatchIds(qids);
                Map<Long, Question> qmap = questions.stream()
                        .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));
                for (QuestionFavorite fav : records) {
                    if (fav.getQuestionId() != null) {
                        fav.setQuestion(qmap.get(fav.getQuestionId()));
                    }
                }
            }
        }
        return result;
    }

    @Override
    public boolean isFavorited(Long userId, Long questionId) {
        LambdaQueryWrapper<QuestionFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionFavorite::getUserId, userId)
               .eq(QuestionFavorite::getQuestionId, questionId);
        Long count = baseMapper.selectCount(wrapper);
        return count != null && count > 0;
    }
}
