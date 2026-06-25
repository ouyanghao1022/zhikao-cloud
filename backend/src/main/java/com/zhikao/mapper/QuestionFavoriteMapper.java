package com.zhikao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhikao.entity.QuestionFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题目收藏Mapper
 */
@Mapper
public interface QuestionFavoriteMapper extends BaseMapper<QuestionFavorite> {
}
