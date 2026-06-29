package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhikao.entity.StudyGroupChat;
import com.zhikao.entity.User;
import com.zhikao.mapper.StudyGroupChatMapper;
import com.zhikao.mapper.UserMapper;
import com.zhikao.service.GroupChatService;
import com.zhikao.service.SensitiveWordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习小组聊天Service实现类
 */
@Service
public class GroupChatServiceImpl implements GroupChatService {

    @Autowired
    private StudyGroupChatMapper groupChatMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired(required = false)
    private SensitiveWordService sensitiveWordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudyGroupChat sendMessage(Long groupId, Long userId, String content,
                                      Integer contentType, String fileUrl) {
        StudyGroupChat chat = new StudyGroupChat();
        chat.setGroupId(groupId);
        chat.setUserId(userId);
        chat.setContentType(contentType != null ? contentType : 1);
        // 文本消息做敏感词过滤（若 SensitiveWordService 已注入）
        if (chat.getContentType() == 1 && content != null && sensitiveWordService != null) {
            content = sensitiveWordService.filter(content);
        }
        chat.setContent(content);
        chat.setFileUrl(fileUrl);
        chat.setCreatedAt(new Date());
        groupChatMapper.insert(chat);
        return chat;
    }

    @Override
    public IPage<StudyGroupChat> listMessages(Long groupId, long current, long size) {
        Page<StudyGroupChat> page = new Page<>(current, size);
        LambdaQueryWrapper<StudyGroupChat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudyGroupChat::getGroupId, groupId)
               .orderByAsc(StudyGroupChat::getCreatedAt);
        IPage<StudyGroupChat> result = groupChatMapper.selectPage(page, wrapper);

        // 填充用户昵称/头像
        List<StudyGroupChat> records = result.getRecords();
        if (records != null && !records.isEmpty()) {
            List<Long> userIds = records.stream().map(StudyGroupChat::getUserId).distinct().toList();
            List<User> users = userMapper.selectBatchIds(userIds);
            Map<Long, User> userMap = new HashMap<>();
            for (User u : users) {
                userMap.put(u.getId(), u);
            }
            for (StudyGroupChat c : records) {
                User u = userMap.get(c.getUserId());
                if (u != null) {
                    c.setNickname(u.getNickname());
                    c.setAvatar(u.getAvatar());
                }
            }
        }
        return result;
    }
}
