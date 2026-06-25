package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * PK队伍实体
 */
@Data
@TableName("pk_team")
public class PkTeam implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String teamName;

    private String slogan;

    private Integer maxMembers;

    private Integer currentMembers;

    private Long captainId;

    private Integer joinType;

    private Integer totalScore;

    private Integer winCount;

    private Integer loseCount;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    /** 队长用户名（非数据库字段） */
    @TableField(exist = false)
    private String captainName;

    /** 队员列表（非数据库字段） */
    @TableField(exist = false)
    private List<PkTeamMember> members;
}
