package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 试卷-题目关联实体
 */
@Data
@TableName("exam_paper_question")
public class ExamPaperQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 试卷ID */
    private Long paperId;

    /** 题目ID */
    private Long questionId;

    /** 排序 */
    private Integer sort;

    /** 本题分值 */
    private BigDecimal score;

    /** 题目数据快照（JSON），题库删除后仍可恢复 */
    private String questionSnapshot;

    /** 创建时间 */
    private Date createdAt;
}
