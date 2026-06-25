package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.SysNotificationSetting;
import com.zhikao.mapper.SysNotificationSettingMapper;
import com.zhikao.service.NotificationSettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;

/**
 * 消息通知设置Service实现类
 */
@Service
public class NotificationSettingServiceImpl
        extends ServiceImpl<SysNotificationSettingMapper, SysNotificationSetting>
        implements NotificationSettingService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysNotificationSetting getOrInit(Long userId) {
        LambdaQueryWrapper<SysNotificationSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotificationSetting::getUserId, userId);
        SysNotificationSetting setting = getOne(wrapper);
        if (setting == null) {
            setting = new SysNotificationSetting();
            setting.setUserId(userId);
            setting.setSystemNotice(1);
            setting.setExamRemind(1);
            setting.setTeamInvite(1);
            setting.setAtRemind(1);
            setting.setReplyRemind(1);
            setting.setPkInvite(1);
            setting.setUpdatedAt(new Date());
            save(setting);
        }
        return setting;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Map<String, Integer> settings) {
        SysNotificationSetting setting = getOrInit(userId);
        if (settings.containsKey("systemNotice")) {
            setting.setSystemNotice(settings.get("systemNotice"));
        }
        if (settings.containsKey("examRemind")) {
            setting.setExamRemind(settings.get("examRemind"));
        }
        if (settings.containsKey("teamInvite")) {
            setting.setTeamInvite(settings.get("teamInvite"));
        }
        if (settings.containsKey("atRemind")) {
            setting.setAtRemind(settings.get("atRemind"));
        }
        if (settings.containsKey("replyRemind")) {
            setting.setReplyRemind(settings.get("replyRemind"));
        }
        if (settings.containsKey("pkInvite")) {
            setting.setPkInvite(settings.get("pkInvite"));
        }
        setting.setUpdatedAt(new Date());
        updateById(setting);
    }
}
