package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 答题记录实体
 */
@Data
@TableName("exam_answer")
public class ExamAnswer implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 考试记录ID */
    private Long sessionId;

    /** 题目ID */
    private Long questionId;

    /** 用户答案 */
    private String userAnswer;

    /** 是否正确：0否 1是 */
    private Integer isCorrect;

    /** 本题得分 */
    private java.math.BigDecimal score;

    /** 批改人ID */
    private Long correctedBy;

    /** 批改时间 */
    private Date correctedAt;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
}
