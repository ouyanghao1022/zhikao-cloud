package com.zhikao.controller;

import com.zhikao.common.Result;
import com.zhikao.entity.SysNotificationSetting;
import com.zhikao.service.NotificationSettingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 消息通知设置控制器
 */
@RestController
@RequestMapping("/notification")
public class NotificationSettingController {

    @Autowired
    private NotificationSettingService notificationSettingService;

    /**
     * 获取当前用户的通知设置
     */
    @GetMapping("/setting")
    public Result<SysNotificationSetting> getSetting(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(notificationSettingService.getOrInit(userId));
    }

    /**
     * 更新当前用户的通知设置
     */
    @PutMapping("/setting")
    public Result<?> updateSetting(@RequestBody Map<String, Integer> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        notificationSettingService.update(userId, body);
        return Result.success("更新成功");
    }
}
