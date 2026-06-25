package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 题目收藏实体
 */
@Data
@TableName("question_favorite")
public class QuestionFavorite implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 题目ID */
    private Long questionId;

    /** 收藏夹ID */
    private Long folderId;

    /** 收藏笔记 */
    private String note;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    /** 题目信息（非数据库字段，查询时填充） */
    @TableField(exist = false)
    private Question question;
}
