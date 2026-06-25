package com.zhikao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhikao.entity.UserIntegralLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户积分变动日志Mapper
 */
@Mapper
public interface UserIntegralLogMapper extends BaseMapper<UserIntegralLog> {
}
