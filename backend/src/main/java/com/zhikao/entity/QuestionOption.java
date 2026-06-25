package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 题目选项实体（单选/多选/判断）
 */
@Data
@TableName("question_option")
public class QuestionOption implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题目ID */
    private Long questionId;

    /** 选项标签：A/B/C/D */
    private String optionLabel;

    /** 选项内容 */
    private String optionContent;

    /** 是否为正确答案：0否 1是 */
    private Integer isCorrect;

    /** 排序 */
    private Integer sort;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
}
