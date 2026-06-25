package com.zhikao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhikao.entity.FavoriteFolder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收藏夹Mapper
 */
@Mapper
public interface FavoriteFolderMapper extends BaseMapper<FavoriteFolder> {
}
