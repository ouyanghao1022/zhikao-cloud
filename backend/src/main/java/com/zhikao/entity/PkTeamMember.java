package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * PK队伍成员实体
 */
@Data
@TableName("pk_team_member")
public class PkTeamMember implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teamId;

    private Long userId;

    private Integer role;

    private Date joinTime;

    private Integer status;

    /** 用户名（非数据库字段） */
    @TableField(exist = false)
    private String username;

    /** 昵称（非数据库字段） */
    @TableField(exist = false)
    private String nickname;

    /** 队名（非数据库字段） */
    @TableField(exist = false)
    private String teamName;
}
