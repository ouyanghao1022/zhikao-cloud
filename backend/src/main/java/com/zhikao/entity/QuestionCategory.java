package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 题库分类实体（学科/章节/知识点 三级目录）
 */
@Data
@TableName("question_category")
public class QuestionCategory implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父分类ID */
    private Long parentId;

    /** 分类名称 */
    private String categoryName;

    /** 类型：1学科 2章节 3知识点 */
    private Integer categoryType;

    /** 排序 */
    private Integer sort;

    /** 状态：0禁用 1正常 */
    private Integer status;

    /** 是否允许练习：0否 1是 */
    private Integer allowPractice;

    /** 创建者ID（教师隔离） */
    private Long creatorId;

    /** 创建者名称（非数据库字段） */
    @TableField(exist = false)
    private String creatorName;

    /** 创建时间 */
    private Date createdAt;

    /** 更新时间 */
    private Date updatedAt;

    /** 子分类列表（非数据库字段） */
    @TableField(exist = false)
    private List<QuestionCategory> children;
}
