package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 讨论区评论实体
 */
@Data
@TableName("discussion_comment")
public class DiscussionComment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    private Long userId;

    private Long parentId;

    private String content;

    private Integer likeCount;

    private Long replyToUserId;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    /** 评论人用户名（非数据库字段） */
    @TableField(exist = false)
    private String username;

    /** 被回复人用户名（非数据库字段） */
    @TableField(exist = false)
    private String replyToUsername;

    /** 子评论列表（非数据库字段） */
    @TableField(exist = false)
    private List<DiscussionComment> children;
}
