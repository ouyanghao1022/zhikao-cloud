package com.zhikao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhikao.entity.QuestionTagRel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题目标签关联Mapper
 */
@Mapper
public interface QuestionTagRelMapper extends BaseMapper<QuestionTagRel> {
}
