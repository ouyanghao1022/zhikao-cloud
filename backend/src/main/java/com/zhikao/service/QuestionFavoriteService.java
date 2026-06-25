package com.zhikao.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.entity.QuestionFavorite;

/**
 * 题目收藏Service
 */
public interface QuestionFavoriteService extends IService<QuestionFavorite> {

    /**
     * 收藏题目（带可选 note，若已存在则更新 note）
     */
    QuestionFavorite favorite(Long userId, Long questionId, String note);

    /**
     * 取消收藏
     */
    void unfavorite(Long userId, Long questionId);

    /**
     * 我的收藏分页列表（含题目信息）
     */
    IPage<QuestionFavorite> listMyFavorites(Long userId, long current, long size);

    /**
     * 检测是否已收藏
     */
    boolean isFavorited(Long userId, Long questionId);
}
