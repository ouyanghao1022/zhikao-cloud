package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 消息通知设置实体
 */
@Data
@TableName("sys_notification_setting")
public class SysNotificationSetting implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 系统通知：0关闭 1开启 */
    private Integer systemNotice;

    /** 考试提醒：0关闭 1开启 */
    private Integer examRemind;

    /** 组队邀请：0关闭 1开启 */
    private Integer teamInvite;

    /** @提醒：0关闭 1开启 */
    private Integer atRemind;

    /** 回复提醒：0关闭 1开启 */
    private Integer replyRemind;

    /** PK邀请：0关闭 1开启 */
    private Integer pkInvite;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
}
