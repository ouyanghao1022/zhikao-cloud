package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 学习小组实体
 */
@Data
@TableName("study_group")
public class StudyGroup implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String groupName;

    private String description;

    private String tags;

    private String coverUrl;

    private Integer joinType;

    private Integer maxMembers;

    private Integer currentMembers;

    private Long creatorId;

    private Integer level;

    private Integer activeScore;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;



    /** 创建人用户名（非数据库字段） */
    @TableField(exist = false)
    private String creatorName;
}
