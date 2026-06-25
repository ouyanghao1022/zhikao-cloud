package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * PK对战答题记录实体
 */
@Data
@TableName("pk_match_record")
public class PkMatchRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long matchId;

    private Long userId;

    private Long teamId;

    private Integer roundNum;

    private Long questionId;

    private String answer;

    private Integer isCorrect;

    private Integer answerTimeMs;

    private Integer score;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    /** 用户名（非数据库字段） */
    @TableField(exist = false)
    private String username;
}
