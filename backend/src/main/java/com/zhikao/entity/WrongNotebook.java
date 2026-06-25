package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 错题本实体
 */
@Data
@TableName("wrong_notebook")
public class WrongNotebook implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 题目ID */
    private Long questionId;

    /** 关联考试记录ID */
    private Long examSessionId;

    /** 错误答案 */
    private String wrongAnswer;

    /** 正确答案 */
    private String correctAnswer;

    /** 错误类型：0概念不清 1粗心大意 2完全不会 3审题错误 */
    private Integer errorType;

    /** 掌握状态：0未掌握 1模糊 2已掌握 */
    private Integer masterStatus;

    /** 复习次数 */
    private Integer reviewCount;

    /** 最后复习时间 */
    private Date lastReviewTime;

    /** 下次复习时间(遗忘曲线) */
    private Date nextReviewTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

}
