package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 题库实体
 */
@Data
@TableName("question_bank")
public class Question implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属分类ID */
    private Long categoryId;

    /** 题型：1单选 2多选 3判断 4填空 5简答 6论述 7编程 8组合题 */
    private Integer questionType;

    /** 难度：1简单 2中等 3困难 4极难 */
    private Integer difficulty;

    /** 题目标题/题干 */
    private String title;

    /** 题目详情（材料分析题材料） */
    private String content;

    /** 正确答案 */
    private String answer;

    /** 解析内容 */
    private String answerAnalysis;

    /** 视频解析链接 */
    private String videoAnalysisUrl;

    /** 是否历年真题：0否 1是 */
    private Integer isPastExam;

    /** 题目来源 */
    private String source;

    /** 使用次数 */
    private Integer usageCount;

    /** 正确率(%) */
    private java.math.BigDecimal correctRate;

    /** 题目分值 */
    private java.math.BigDecimal score;

    /** 创建人ID */
    private Long creatorId;

    /** 题库类型：1公开 2私有 3机构共享 */
    private Integer bankType;

    /** 状态：0禁用 1正常 */
    private Integer status;

    /** 是否允许练习：0否 1是 */
    private Integer allowPractice;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;


    /** 题目选项列表（非数据库字段） */
    @TableField(exist = false)
    private List<QuestionOption> options;

    /** 所属分类名称（非数据库字段，查询时填充） */
    @TableField(exist = false)
    private String categoryName;
}
