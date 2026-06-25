package com.zhikao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.entity.ExamSession;

/**
 * 考试记录Service
 */
public interface ExamSessionService extends IService<ExamSession> {

    /**
     * 获取或创建考试会话
     */
    ExamSession getOrCreateSession(Long paperId, Long userId);
}
