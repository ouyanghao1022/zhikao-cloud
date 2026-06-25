package com.zhikao.service;

import com.zhikao.entity.ExamCertificate;

import java.util.List;

/**
 * 考试证书Service
 */
public interface ExamCertificateService {

    /** 颁发证书（已通过且未颁发过则颁发，否则返回已有/抛异常） */
    ExamCertificate issue(Long sessionId);

    /** 按考试记录查询证书 */
    ExamCertificate getBySession(Long sessionId);

    /** 查询用户全部证书 */
    List<ExamCertificate> listByUser(Long userId);
}
