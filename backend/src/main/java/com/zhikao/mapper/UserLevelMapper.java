package com.zhikao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhikao.entity.UserLevel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户等级配置Mapper
 */
@Mapper
public interface UserLevelMapper extends BaseMapper<UserLevel> {
}
