package com.zhikao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.entity.ExamPaper;
import java.util.List;
import java.util.Map;

/**
 * 试卷Service接口
 */
public interface ExamPaperService extends IService<ExamPaper> {

    /**
     * 分页查询试卷
     */
    com.baomidou.mybatisplus.core.metadata.IPage<ExamPaper> pageList(long current, long size,
                                                                     Integer status, Long categoryId, Long classId,
                                                                     Long creatorId);

    /**
     * 学生端分页查询（只显示发布到学生所在班级的考试）
     */
    com.baomidou.mybatisplus.core.metadata.IPage<ExamPaper> pageListForStudent(long current, long size, Long studentId);

    /**
     * 获取试卷详情（含题目列表）
     */
    Map<String, Object> getPaperDetail(Long paperId);

    /**
     * 创建试卷
     * @param paper 试卷基本信息
     * @param questionIds 固定组卷：题目ID列表
     * @param questionScores 固定组卷：每题分值
     * @param randomConfig 随机组卷配置
     */
    void createPaper(ExamPaper paper, java.util.List<Long> questionIds,
                     java.util.List<String> questionScores,
                     java.util.Map<String, Object> randomConfig);

    /**
     * 重新抽题（仅草稿随机试卷）
     */
    Map<String, Object> reassembleRandomPaper(Long paperId);

    /**
     * 编辑试卷（基本字段 + 草稿固定卷可替换题目；已发布/有作答仅允许改 endTime/说明）
     */
    void updatePaper(Long paperId, ExamPaper paper,
                     java.util.List<Long> questionIds, java.util.List<String> questionScores);

    /**
     * 发布试卷（绑定到指定班级）
     */
    void publishPaper(Long paperId, List<Long> classIds);

    /**
     * 获取考试答题数据（学生参加考试时调用）
     */
    Map<String, Object> getExamForTaking(Long paperId, Long userId);

    /**
     * 提交考试答案
     */
    Map<String, Object> submitExam(Long paperId, Long userId, java.util.List<Map<String, Object>> answers);

    /**
     * 删除试卷（含关联题目）
     */
    void deletePaper(Long paperId);

    /**
     * 查询我的考试记录
     */
    com.baomidou.mybatisplus.core.metadata.IPage<Map<String, Object>> getMyRecords(Long userId, long current, long size);
}
