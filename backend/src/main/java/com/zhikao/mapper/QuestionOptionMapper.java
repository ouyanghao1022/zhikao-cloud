package com.zhikao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhikao.entity.QuestionOption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 题目选项Mapper
 */
@Mapper
public interface QuestionOptionMapper extends BaseMapper<QuestionOption> {

    /**
     * 根据题目ID查询选项列表
     */
    @Select("SELECT * FROM question_option WHERE question_id = #{questionId} ORDER BY sort ASC")
    List<QuestionOption> selectByQuestionId(Long questionId);
}
