package com.zhikao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhikao.entity.ExamPaper;
import com.zhikao.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 试卷Mapper
 */
@Mapper
public interface ExamPaperMapper extends BaseMapper<ExamPaper> {

    /**
     * 根据试卷ID查询关联题目列表（试卷中的分值覆盖默认分值）
     */
    @Select("SELECT q.id, q.category_id, q.question_type, q.difficulty, q.title, q.content, " +
            "q.answer, q.answer_analysis, q.video_analysis_url, q.is_past_exam, q.source, " +
            "q.usage_count, q.correct_rate, epq.score as score, q.creator_id, " +
            "q.bank_type, q.status, q.created_at, q.updated_at " +
            "FROM question_bank q " +
            "INNER JOIN exam_paper_question epq ON q.id = epq.question_id " +
            "WHERE epq.paper_id = #{paperId} ORDER BY epq.sort ASC")
    List<Question> selectQuestionsByPaperId(@Param("paperId") Long paperId);

    /**
     * 查询用户的考试记录
     */
    @Select("SELECT es.*, ep.title as paperTitle FROM exam_session es " +
            "INNER JOIN exam_paper ep ON es.paper_id = ep.id " +
            "WHERE es.user_id = #{userId} ORDER BY es.created_at DESC")
    IPage<Map<String, Object>> selectMyRecords(@Param("userId") Long userId, Page<?> page);

    /**
     * 获取试卷中某题的设定分值
     */
    @Select("SELECT score FROM exam_paper_question WHERE paper_id = #{paperId} AND question_id = #{questionId}")
    BigDecimal getQuestionScore(@Param("paperId") Long paperId, @Param("questionId") Long questionId);
}
