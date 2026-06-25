package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 排行榜实体
 */
@Data
@TableName("leaderboard")
public class Leaderboard implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer leaderboardType;

    private Integer periodType;

    private String periodKey;

    private Long userId;

    private Long teamId;

    private Integer score;

    private Integer rankNum;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    /** 用户名（非数据库字段） */
    @TableField(exist = false)
    private String username;

    /** 昵称（非数据库字段） */
    @TableField(exist = false)
    private String nickname;

    /** 队名（非数据库字段） */
    @TableField(exist = false)
    private String teamName;

    /** 头像（非数据库字段） */
    @TableField(exist = false)
    private String avatar;
}
