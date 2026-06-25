package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 讨论区关注实体
 */
@Data
@TableName("discussion_follow")
public class DiscussionFollow implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long followUserId;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
}
