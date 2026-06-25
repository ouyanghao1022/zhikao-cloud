package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.ExamCertificate;
import com.zhikao.entity.ExamPaper;
import com.zhikao.entity.ExamSession;
import com.zhikao.mapper.ExamCertificateMapper;
import com.zhikao.mapper.ExamPaperMapper;
import com.zhikao.mapper.UserMapper;
import com.zhikao.service.ExamCertificateService;
import com.zhikao.service.ExamSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 考试证书Service实现
 */
@Service
public class ExamCertificateServiceImpl extends ServiceImpl<ExamCertificateMapper, ExamCertificate>
        implements ExamCertificateService {

    @Autowired
    private ExamSessionService examSessionService;

    @Autowired
    private ExamPaperMapper examPaperMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamCertificate issue(Long sessionId) {
        // 已颁发则直接返回
        ExamCertificate existing = getBySession(sessionId);
        if (existing != null) return existing;

        ExamSession session = examSessionService.getById(sessionId);
        if (session == null) {
            throw new RuntimeException("考试记录不存在");
        }
        ExamPaper paper = examPaperMapper.selectById(session.getPaperId());
        if (paper == null) {
            throw new RuntimeException("试卷不存在");
        }
        BigDecimal pass = paper.getPassScore() != null ? paper.getPassScore() : BigDecimal.ZERO;
        BigDecimal score = session.getTotalScore() != null ? session.getTotalScore() : BigDecimal.ZERO;
        if (score.compareTo(pass) < 0) {
            throw new RuntimeException("未达到及格分，无法颁发证书");
        }

        ExamCertificate cert = new ExamCertificate();
        cert.setSessionId(sessionId);
        cert.setUserId(session.getUserId());
        cert.setPaperId(session.getPaperId());
        cert.setCertNo("ZK" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 999));
        cert.setCertUrl("/cert/by-session/" + sessionId);
        cert.setIssueTime(new Date());
        save(cert);
        return cert;
    }

    @Override
    public ExamCertificate getBySession(Long sessionId) {
        return getOne(new LambdaQueryWrapper<ExamCertificate>()
                .eq(ExamCertificate::getSessionId, sessionId));
    }

    @Override
    public List<ExamCertificate> listByUser(Long userId) {
        return list(new LambdaQueryWrapper<ExamCertificate>()
                .eq(ExamCertificate::getUserId, userId)
                .orderByDesc(ExamCertificate::getIssueTime));
    }
}
