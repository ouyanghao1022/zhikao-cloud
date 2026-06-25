package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.ClassMember;
import com.zhikao.entity.ExamSession;
import com.zhikao.mapper.ClassMemberMapper;
import com.zhikao.mapper.ExamSessionMapper;
import com.zhikao.service.ExamSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 考试记录Service实现
 */
@Service
public class ExamSessionServiceImpl extends ServiceImpl<ExamSessionMapper, ExamSession> implements ExamSessionService {

    @Autowired
    private ClassMemberMapper classMemberMapper;

    @Override
    public ExamSession getOrCreateSession(Long paperId, Long userId) {
        LambdaQueryWrapper<ExamSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamSession::getPaperId, paperId)
               .eq(ExamSession::getUserId, userId);
        ExamSession session = getOne(wrapper);
        if (session == null) {
            session = new ExamSession();
            session.setPaperId(paperId);
            session.setUserId(userId);
            session.setStartTime(new Date());
            session.setStatus(0); // 进行中
            session.setCreatedAt(new Date());
            session.setUpdatedAt(new Date());

            // 记录学生所属班级（考试开始时从class_member获取）
            try {
                LambdaQueryWrapper<ClassMember> cmWrapper = new LambdaQueryWrapper<>();
                cmWrapper.eq(ClassMember::getUserId, userId)
                         .eq(ClassMember::getStatus, 1)
                         .orderByDesc(ClassMember::getJoinTime)
                         .last("LIMIT 1");
                ClassMember member = classMemberMapper.selectOne(cmWrapper);
                if (member != null) {
                    session.setClassId(member.getClassId());
                }
            } catch (Exception ignored) {
                // 班级查询失败不影响考试
            }

            save(session);
        } else if (session.getClassId() == null) {
            // 已有会话但未记录班级，补充记录
            try {
                LambdaQueryWrapper<ClassMember> cmWrapper = new LambdaQueryWrapper<>();
                cmWrapper.eq(ClassMember::getUserId, userId)
                         .eq(ClassMember::getStatus, 1)
                         .orderByDesc(ClassMember::getJoinTime)
                         .last("LIMIT 1");
                ClassMember member = classMemberMapper.selectOne(cmWrapper);
                if (member != null) {
                    session.setClassId(member.getClassId());
                    updateById(session);
                }
            } catch (Exception ignored) {
                // ignore
            }
        }
        return session;
    }
}
