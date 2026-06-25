package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 系统用户实体
 */
@Data
@TableName("sys_user")
public class User implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** 密码(加密) */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 头像URL */
    private String avatar;

    /** 性别：0未知 1男 2女 */
    private Integer gender;

    /** 学校 */
    private String school;

    /** 年级 */
    private String grade;

    /** 个性签名 */
    private String signature;

    /** 状态：0禁用 1正常 */
    private Integer status;

    /** 登录次数 */
    private Integer loginCount;

    /** 最后登录时间 */
    private Date lastLoginTime;

    /** 最后登录IP */
    private String lastLoginIp;

    /** 第三方OpenID */
    private String openId;

    /** 第三方类型 */
    private String oauthType;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;


    /** 用户角色列表（非数据库字段） */
    @TableField(exist = false)
    private List<Role> roles;
}
