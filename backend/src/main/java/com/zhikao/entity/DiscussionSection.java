package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 讨论区版块实体
 */
@Data
@TableName("discussion_section")
public class DiscussionSection implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sectionName;

    private String description;

    private Integer sectionType;

    private String icon;

    private Integer sort;

    private Integer postCount;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
}
