package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 学习小组成员实体
 */
@Data
@TableName("study_group_member")
public class StudyGroupMember implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private Long userId;

    /** 角色：0成员 1管理员 2组长 */
    private Integer role;

    private Integer contribution;

    private Date joinTime;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;


    /** 用户名（非数据库字段） */
    @TableField(exist = false)
    private String username;

    /** 昵称（非数据库字段） */
    @TableField(exist = false)
    private String nickname;

    /** 头像（非数据库字段） */
    @TableField(exist = false)
    private String avatar;
}
