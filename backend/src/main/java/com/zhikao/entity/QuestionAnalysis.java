package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 题目解析实体
 */
@Data
@TableName("question_analysis")
public class QuestionAnalysis implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题目ID（唯一） */
    private Long questionId;

    /** 文字解析 */
    private String textAnalysis;

    /** 视频解析链接 */
    private String videoUrl;

    /** 涉及知识点，逗号分隔 */
    private String knowledgePoints;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
}
