package com.zhikao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhikao.entity.Question;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题库Mapper
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}
