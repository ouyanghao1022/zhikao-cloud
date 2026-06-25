package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("sys_message")
public class SysMessage implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人ID */
    private Long receiverId;

    /** 发送人ID(系统消息为NULL) */
    private Long senderId;

    /** 消息类型：1系统通知 2考试提醒 3组队邀请 4@提醒 5回复提醒 6PK邀请 */
    private Integer messageType;

    /** 消息标题 */
    private String title;

    /** 消息内容 */
    private String content;

    /** 关联ID */
    private Long relatedId;

    /** 关联链接 */
    private String relatedUrl;

    /** 是否已读：0未读 1已读 */
    private Integer isRead;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    // 非数据库字段
    @TableField(exist = false)
    private String typeText;
}
