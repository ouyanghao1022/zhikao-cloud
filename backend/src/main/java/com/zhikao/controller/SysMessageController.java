package com.zhikao.controller;

import com.zhikao.common.Result;
import com.zhikao.entity.SysMessage;
import com.zhikao.service.SysMessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/messages")
public class SysMessageController {

    @Autowired
    private SysMessageService sysMessageService;

    /** 获取消息列表 */
    @GetMapping("/list")
    public Result<List<SysMessage>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(sysMessageService.getUserMessages(userId));
    }

    /** 获取未读消息数 */
    @GetMapping("/unread-count")
    public Result<Map<String, Integer>> unreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Integer> data = new HashMap<>();
        data.put("count", sysMessageService.countUnread(userId));
        return Result.success(data);
    }

    /** 标记单条已读 */
    @PutMapping("/read/{id}")
    public Result<Void> markRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        sysMessageService.markRead(id, userId);
        return Result.success();
    }

    /** 标记全部已读 */
    @PutMapping("/read-all")
    public Result<Void> markAllRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        sysMessageService.markAllRead(userId);
        return Result.success();
    }
}
