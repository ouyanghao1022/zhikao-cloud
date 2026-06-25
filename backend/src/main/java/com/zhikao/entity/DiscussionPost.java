package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 讨论区帖子实体
 */
@Data
@TableName("discussion_post")
public class DiscussionPost implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sectionId;

    private Long userId;

    private String title;

    private String content;

    private Integer contentType;

    private String tags;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Integer isTop;

    private Integer isEssence;

    private Integer status;

    /** 审核状态：0待审核 1通过 2不通过 */
    private Integer isAuditPassed;

    /** 审核意见 */
    private String auditMsg;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    /** 发帖人用户名（非数据库字段） */
    @TableField(exist = false)
    private String username;

    /** 发帖人昵称（非数据库字段） */
    @TableField(exist = false)
    private String nickname;

    /** 版块名称（非数据库字段） */
    @TableField(exist = false)
    private String sectionName;

    /** 是否已点赞（非数据库字段） */
    @TableField(exist = false)
    private Boolean isLiked;

    /** 审核状态文本（非数据库字段） */
    @TableField(exist = false)
    private String auditStatusText;
}
