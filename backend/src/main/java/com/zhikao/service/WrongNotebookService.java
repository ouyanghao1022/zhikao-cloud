package com.zhikao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.entity.WrongNotebook;

import java.util.List;
import java.util.Map;

/**
 * 错题本Service
 */
public interface WrongNotebookService extends IService<WrongNotebook> {

    /**
     * 添加错题到错题本
     */
    WrongNotebook addWrongNote(Long userId, Long questionId, Long examSessionId,
                                String wrongAnswer, String correctAnswer);

    /**
     * 给错题打标签（重复忽略）
     */
    void tagWrongNote(Long noteId, List<String> tags);

    /**
     * 查错题标签
     */
    List<String> listWrongNoteTags(Long noteId);

    /**
     * 艾宾浩斯遗忘曲线：按复习次数返回下次复习间隔天数
     */
    int calcNextReviewTime(int reviewCount);

    /**
     * 生成今日复习计划（聚合写入 wrong_review_plan）
     */
    Map<String, Object> generateTodayPlan(Long userId);

    /**
     * 标记复习完成
     */
    WrongNotebook markReviewed(Long noteId);

    /**
     * 获取今日计划（含应复习数、已完成数、待复习错题列表）
     */
    Map<String, Object> getTodayPlan(Long userId);
}
