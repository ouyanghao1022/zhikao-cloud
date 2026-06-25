package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 讨论区点赞实体
 */
@Data
@TableName("discussion_like")
public class DiscussionLike implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer targetType;

    private Long targetId;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    /** 点赞人用户名（非数据库字段） */
    @TableField(exist = false)
    private String username;
}
