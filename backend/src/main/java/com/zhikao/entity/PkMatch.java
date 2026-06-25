package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * PK对战实体
 */
@Data
@TableName("pk_match")
public class PkMatch implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String matchNo;

    private Integer matchType;

    private Long teamAId;

    private Long teamBId;

    private Long paperId;

    private Integer status;

    private Date startTime;

    private Date endTime;

    private Long winnerTeamId;

    private Integer totalRounds;

    private Integer currentRound;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    /** A队名称（非数据库字段） */
    @TableField(exist = false)
    private String teamAName;

    /** B队名称（非数据库字段） */
    @TableField(exist = false)
    private String teamBName;

    /** 获胜队伍名称（非数据库字段） */
    @TableField(exist = false)
    private String winnerTeamName;
}
