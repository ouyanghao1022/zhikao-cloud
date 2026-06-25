package com.zhikao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhikao.entity.FavoriteItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收藏项Mapper
 */
@Mapper
public interface FavoriteItemMapper extends BaseMapper<FavoriteItem> {
}
