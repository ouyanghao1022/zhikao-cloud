package com.zhikao.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhikao.entity.StudyGroupChat;

/**
 * 学习小组聊天Service接口
 */
public interface GroupChatService {

    /**
     * 发送聊天消息
     *
     * @param groupId      小组ID
     * @param userId       发送人ID
     * @param content      消息内容
     * @param contentType  内容类型：1文本 2图片 3文件
     * @param fileUrl      文件URL
     * @return 保存后的消息
     */
    StudyGroupChat sendMessage(Long groupId, Long userId, String content,
                               Integer contentType, String fileUrl);

    /**
     * 分页查询小组聊天记录（按时间升序），带用户昵称/头像
     */
    IPage<StudyGroupChat> listMessages(Long groupId, long current, long size);
}
