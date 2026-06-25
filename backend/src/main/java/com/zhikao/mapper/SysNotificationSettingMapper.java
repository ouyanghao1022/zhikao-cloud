package com.zhikao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhikao.entity.SysNotificationSetting;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息通知设置Mapper
 */
@Mapper
public interface SysNotificationSettingMapper extends BaseMapper<SysNotificationSetting> {
}
