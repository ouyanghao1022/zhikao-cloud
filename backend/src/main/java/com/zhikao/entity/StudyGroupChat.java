package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 学习小组聊天记录实体
 */
@Data
@TableName("study_group_chat")
public class StudyGroupChat implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 小组ID */
    private Long groupId;

    /** 用户ID */
    private Long userId;

    /** 消息内容 */
    private String content;

    /** 内容类型：1文本 2图片 3文件 */
    private Integer contentType;

    /** 文件URL */
    private String fileUrl;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    /** 发送人昵称（非数据库字段，查询时填充） */
    @TableField(exist = false)
    private String nickname;

    /** 发送人头像（非数据库字段，查询时填充） */
    @TableField(exist = false)
    private String avatar;
}
