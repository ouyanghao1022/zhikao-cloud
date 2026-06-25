package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 错题标签实体
 */
@Data
@TableName("wrong_note_tag")
public class WrongNoteTag implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 错题ID */
    private Long wrongNoteId;

    /** 标签名称 */
    private String tagName;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
}
