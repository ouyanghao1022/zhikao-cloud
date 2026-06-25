package com.zhikao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.entity.SysMessage;
import java.util.List;
import java.util.Map;

public interface SysMessageService extends IService<SysMessage> {

    /** 获取用户消息列表 */
    List<SysMessage> getUserMessages(Long userId);

    /** 获取未读消息数 */
    int countUnread(Long userId);

    /** 标记单条已读 */
    void markRead(Long messageId, Long userId);

    /** 标记全部已读 */
    void markAllRead(Long userId);

    /** 发送通知 */
    void sendMessage(Long receiverId, Integer messageType, String title, String content, Long relatedId, String relatedUrl);
}
