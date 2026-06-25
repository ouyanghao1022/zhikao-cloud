package com.zhikao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.entity.Question;
import java.util.List;

/**
 * 题库Service接口
 */
public interface QuestionService extends IService<Question> {

    /**
     * 智能随机组卷（按条件抽题）
     */
    List<Question> randomPaper(Long categoryId, Integer totalCount,
                               java.util.Map<Integer, Integer> typeCountMap,
                               java.util.Map<Integer, Integer> difficultyPercentMap,
                               java.util.List<Long> creatorIds);

    /**
     * 分页查询题库
     * @param creatorIds 非空时按创建者过滤（教师自己的题目；admin选定的老师）
     */
    com.baomidou.mybatisplus.core.metadata.IPage<Question> pageList(long current, long size,
                                                                     Long categoryId, Integer questionType,
                                                                     Integer difficulty, String keyword,
                                                                     java.util.List<Long> creatorIds);
}
