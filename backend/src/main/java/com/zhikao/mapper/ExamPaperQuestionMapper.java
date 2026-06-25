package com.zhikao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhikao.entity.ExamPaperQuestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 试卷-题目关联Mapper
 */
@Mapper
public interface ExamPaperQuestionMapper extends BaseMapper<ExamPaperQuestion> {
}
