package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 试卷实体
 */
@Data
@TableName("exam_paper")
public class ExamPaper implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 试卷标题 */
    private String title;

    /** 试卷描述 */
    private String description;

    /** 分类ID */
    private Long categoryId;

    /** 总分 */
    private java.math.BigDecimal totalScore;

    /** 及格分 */
    private java.math.BigDecimal passScore;

    /** 考试时长(分钟) */
    private Integer duration;

    /** 开始时间 */
    private Date startTime;

    /** 结束时间 */
    private Date endTime;

    /** 试卷类型：1固定试卷 2随机试卷 */
    private Integer paperType;

    /** 题目乱序：0否 1是 */
    private Integer shuffleQuestion;

    /** 选项乱序：0否 1是 */
    private Integer shuffleOption;

    /** 最大作答次数 */
    private Integer maxAttempts;

    /** 允许查看答案：0否 1是 */
    private Integer allowViewAnswer;

    /** 成绩显示策略：1交卷即显示 2统一时间显示 3不显示 */
    private Integer showScoreType;

    /** 防作弊等级 */
    private Integer antiCheatLevel;

    /** 最大切屏次数 */
    private Integer maxScreenSwitch;

    /** 创建人ID */
    private Long creatorId;

    /** 创建人名称（非数据库字段，查询时填充） */
    @TableField(exist = false)
    private String creatorName;

    /** 状态：0草稿 1发布 2已结束 3已归档 */
    private Integer status;

    /** 参加人数 */
    private Integer enrolledCount;

    /** 平均分 */
    private java.math.BigDecimal avgScore;

    /** 最高分 */
    private java.math.BigDecimal maxScore;

    /** 最低分 */
    private java.math.BigDecimal minScore;

    /** 证书模板路径 */
    private String certificateTemplate;

    /** 出题老师id逗号分隔（admin选定的老师；教师创建=自己id） */
    private String teacherIds;

    /** 随机组卷配置JSON，用于"重新抽题" */
    private String randomConfig;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

}
