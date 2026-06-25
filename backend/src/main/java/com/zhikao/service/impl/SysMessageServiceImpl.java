package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.SysMessage;
import com.zhikao.mapper.SysMessageMapper;
import com.zhikao.service.SysMessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class SysMessageServiceImpl extends ServiceImpl<SysMessageMapper, SysMessage> implements SysMessageService {

    private static final String[] TYPE_NAMES = {"", "系统通知", "考试提醒", "组队邀请", "@提醒", "回复提醒", "PK邀请"};

    @Override
    public List<SysMessage> getUserMessages(Long userId) {
        LambdaQueryWrapper<SysMessage> qw = new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getReceiverId, userId)
                .orderByDesc(SysMessage::getCreatedAt);
        List<SysMessage> list = baseMapper.selectList(qw);
        for (SysMessage m : list) {
            int t = m.getMessageType();
            m.setTypeText(t > 0 && t < TYPE_NAMES.length ? TYPE_NAMES[t] : "未知");
        }
        return list;
    }

    @Override
    public int countUnread(Long userId) {
        return Math.toIntExact(baseMapper.selectCount(
                new LambdaQueryWrapper<SysMessage>()
                        .eq(SysMessage::getReceiverId, userId)
                        .eq(SysMessage::getIsRead, 0)
        ));
    }

    @Override
    @Transactional
    public void markRead(Long messageId, Long userId) {
        SysMessage msg = getById(messageId);
        if (msg != null && msg.getReceiverId().equals(userId)) {
            msg.setIsRead(1);
            updateById(msg);
        }
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        baseMapper.markAllRead(userId);
    }

    @Override
    @Transactional
    public void sendMessage(Long receiverId, Integer messageType, String title, String content, Long relatedId, String relatedUrl) {
        SysMessage msg = new SysMessage();
        msg.setReceiverId(receiverId);
        msg.setMessageType(messageType);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setRelatedId(relatedId);
        msg.setRelatedUrl(relatedUrl);
        msg.setIsRead(0);
        baseMapper.insert(msg);
    }
}
