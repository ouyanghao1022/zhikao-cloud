package com.zhikao.service;

import java.util.Map;

/**
 * 学情报告Service接口
 */
public interface ReportService {

    /**
     * 个人能力模型（雷达图数据）
     */
    Map<String, Object> getPersonalReport(Long userId);

    /**
     * 考试成绩趋势（折线图数据）
     */
    Map<String, Object> getExamTrend(Long userId);

    /**
     * 知识点掌握热力图
     */
    Map<String, Object> getKnowledgeHeatmap(Long userId);

    /**
     * 薄弱知识点分析
     */
    Map<String, Object> getWeakAreas(Long userId);

    /**
     * 班级成绩分布（教师端，按班级过滤）
     */
    Map<String, Object> getClassReport(Long paperId, Long classId);
}
