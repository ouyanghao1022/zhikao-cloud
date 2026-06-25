package com.zhikao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.entity.SysNotificationSetting;

import java.util.Map;

/**
 * 消息通知设置Service接口
 */
public interface NotificationSettingService extends IService<SysNotificationSetting> {

    /**
     * 获取用户通知设置，无则创建默认全1记录并返回
     */
    SysNotificationSetting getOrInit(Long userId);

    /**
     * 按用户ID更新各开关
     */
    void update(Long userId, Map<String, Integer> settings);
}
