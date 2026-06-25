package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 班级成员实体
 */
@Data
@TableName("class_member")
public class ClassMember implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 班级ID */
    private Long classId;

    /** 用户ID */
    private Long userId;

    /** 角色：0学生 1教师(创建者) */
    private Integer role;

    /** 加入时间 */
    private Date joinTime;

    /** 状态：0已退出 1正常 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    /** 用户名（非数据库字段） */
    @TableField(exist = false)
    private String username;

    /** 昵称（非数据库字段） */
    @TableField(exist = false)
    private String nickname;
}
