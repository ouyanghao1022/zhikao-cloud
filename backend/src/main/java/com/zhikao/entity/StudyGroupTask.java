package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 学习小组任务实体
 */
@Data
@TableName("study_group_task")
public class StudyGroupTask implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private Long creatorId;

    private String taskTitle;

    private String taskContent;

    private Date deadline;

    private Integer completedCount;

    /** 状态：0已取消 1进行中 2已结束 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

}
