package com.zhikao.mapper;

import com.zhikao.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 学情报告Mapper - 聚合查询
 */
@Mapper
public interface ReportMapper {

    /**
     * 查询某用户已完成考试的成绩趋势（按时间排序）
     */
    @Select("SELECT es.id as session_id, es.total_score, es.submit_time, " +
            "COALESCE(ep.title, '未知试卷') as paperTitle, " +
            "COALESCE(ep.total_score, 100) as paper_total_score, " +
            "COALESCE(ep.id, 0) as paper_id " +
            "FROM exam_session es " +
            "LEFT JOIN exam_paper ep ON es.paper_id = ep.id " +
            "WHERE es.user_id = #{userId} AND es.status IN (1,2,3) " +
            "ORDER BY es.submit_time ASC")
    List<Map<String, Object>> selectExamTrend(@Param("userId") Long userId);

    /**
     * 查询用户各知识分类的正确率（用于能力雷达图）
     * 通过题目→题库分类→父分类（学科）获取学科维度的正确率
     */
    @Select("SELECT COALESCE(pc.id, qc.id) as category_id, " +
            "COALESCE(pc.category_name, qc.category_name) as category_name, " +
            "COUNT(ea.id) as total_count, " +
            "SUM(ea.is_correct) as correct_count, " +
            "ROUND(SUM(ea.is_correct) * 100.0 / COUNT(ea.id), 2) as correct_rate " +
            "FROM exam_answer ea " +
            "INNER JOIN exam_session es ON ea.session_id = es.id " +
            "INNER JOIN question_bank qb ON ea.question_id = qb.id " +
            "INNER JOIN question_category qc ON qb.category_id = qc.id " +
            "LEFT JOIN question_category pc ON qc.parent_id = pc.id AND qc.parent_id > 0 " +
            "WHERE es.user_id = #{userId} AND es.status IN (1,2,3) " +
            "GROUP BY COALESCE(pc.id, qc.id), COALESCE(pc.category_name, qc.category_name)")
    List<Map<String, Object>> selectCategoryPerformance(@Param("userId") Long userId);

    /**
     * 查询用户各知识分类的掌握情况（用于热力图）
     * 题库分类维度（category_type=2），题目直接关联
     */
    @Select("SELECT qc.id as knowledge_id, qc.category_name as knowledge_name, " +
            "qc.parent_id as parent_id, qc.category_type as category_type, " +
            "COUNT(ea.id) as total_count, " +
            "SUM(ea.is_correct) as correct_count, " +
            "ROUND(SUM(ea.is_correct) * 100.0 / COUNT(ea.id), 2) as correct_rate " +
            "FROM exam_answer ea " +
            "INNER JOIN exam_session es ON ea.session_id = es.id " +
            "INNER JOIN question_bank qb ON ea.question_id = qb.id " +
            "INNER JOIN question_category qc ON qb.category_id = qc.id " +
            "WHERE es.user_id = #{userId} AND es.status IN (1,2,3) AND qc.category_type IN (1,2) " +
            "GROUP BY qc.id, qc.category_name, qc.parent_id, qc.category_type " +
            "HAVING COUNT(ea.id) >= 1 " +
            "ORDER BY correct_rate ASC")
    List<Map<String, Object>> selectKnowledgeHeatmap(@Param("userId") Long userId);

    /**
     * 查询用户的薄弱知识分类（正确率低于60%的题库分类）
     * 题库分类维度（category_type=2）
     */
    @Select("SELECT qc.id as knowledge_id, qc.category_name as knowledge_name, " +
            "COUNT(ea.id) as total_count, " +
            "SUM(ea.is_correct) as correct_count, " +
            "ROUND(SUM(ea.is_correct) * 100.0 / COUNT(ea.id), 2) as correct_rate, " +
            "wn.error_type, wn.master_status, " +
            "COUNT(DISTINCT wn.id) as wrong_count " +
            "FROM exam_answer ea " +
            "INNER JOIN exam_session es ON ea.session_id = es.id " +
            "INNER JOIN question_bank qb ON ea.question_id = qb.id " +
            "INNER JOIN question_category qc ON qb.category_id = qc.id " +
            "LEFT JOIN wrong_notebook wn ON wn.user_id = es.user_id AND wn.question_id = ea.question_id " +
            "WHERE es.user_id = #{userId} AND es.status IN (1,2,3) AND qc.category_type IN (1,2) " +
            "AND ea.is_correct = 0 " +
            "GROUP BY qc.id, qc.category_name, wn.error_type, wn.master_status " +
            "ORDER BY wrong_count DESC, correct_rate ASC")
    List<Map<String, Object>> selectWeakAreas(@Param("userId") Long userId);

    /**
     * 查询某试卷的班级成绩分布（分数段统计，可选按班级过滤）
     */
    @Select("<script>" +
            "SELECT CASE " +
            "WHEN es.total_score &gt;= ep.total_score * 0.9 THEN '优秀(90%+)' " +
            "WHEN es.total_score &gt;= ep.total_score * 0.75 THEN '良好(75-90%)' " +
            "WHEN es.total_score &gt;= ep.total_score * 0.6 THEN '及格(60-75%)' " +
            "ELSE '不及格(&lt;60%)' END as score_range, " +
            "COUNT(*) as count " +
            "FROM exam_session es " +
            "INNER JOIN exam_paper ep ON es.paper_id = ep.id " +
            "WHERE es.paper_id = #{paperId} AND es.status >= 2 " +
            "<if test='classId != null'>AND es.class_id = #{classId}</if> " +
            "GROUP BY score_range" +
            "</script>")
    List<Map<String, Object>> selectClassScoreDistribution(@Param("paperId") Long paperId,
                                                           @Param("classId") Long classId);

    /**
     * 查询某试卷的基本班级统计（可选按班级过滤）
     */
    @Select("<script>" +
            "SELECT COUNT(DISTINCT es.user_id) as student_count, " +
            "ROUND(AVG(es.total_score), 2) as avg_score, " +
            "MAX(es.total_score) as max_score, " +
            "MIN(es.total_score) as min_score, " +
            "ROUND(STD(es.total_score), 2) as std_score " +
            "FROM exam_session es " +
            "WHERE es.paper_id = #{paperId} AND es.status >= 2 " +
            "<if test='classId != null'>AND es.class_id = #{classId}</if>" +
            "</script>")
    Map<String, Object> selectClassStats(@Param("paperId") Long paperId,
                                         @Param("classId") Long classId);

    /**
     * 查询用户错题涉及的知识点统计
     */
    @Select("SELECT qc.id as knowledge_id, qc.category_name as knowledge_name, " +
            "wn.error_type, COUNT(*) as error_count " +
            "FROM wrong_notebook wn " +
            "INNER JOIN question_bank qb ON wn.question_id = qb.id " +
            "INNER JOIN question_category qc ON qb.category_id = qc.id " +
            "WHERE wn.user_id = #{userId} AND qc.category_type = 3 " +
            "GROUP BY qc.id, qc.category_name, wn.error_type " +
            "ORDER BY error_count DESC " +
            "LIMIT 10")
    List<Map<String, Object>> selectWrongKnowledge(@Param("userId") Long userId);
}
