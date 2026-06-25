package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户积分变动日志实体
 */
@Data
@TableName("user_integral_log")
public class UserIntegralLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 变动类型：1考试得分 2每日签到 3连续打卡 4分享 5邀请好友 6消费 */
    private Integer changeType;

    /** 变动值(正增负减) */
    private Integer changeValue;

    /** 变动后积分 */
    private Integer currentValue;

    /** 描述 */
    private String description;

    /** 关联ID */
    private Long relatedId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
}
