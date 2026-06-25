package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户资料扩展实体
 */
@Data
@TableName("user_profile")
public class UserProfile implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 总考试次数 */
    private Integer totalExams;

    /** 平均分 */
    private BigDecimal avgScore;

    /** 胜率(%) */
    private BigDecimal winRate;

    /** 获得徽章数 */
    private Integer totalBadges;

    /** 等级 */
    private Integer level;

    /** 经验值 */
    private Integer experience;

    /** 积分 */
    private Integer integration;

    /** 连续签到天数 */
    private Integer continuousSignDays;

    /** 最后签到日期 */
    private Date lastSignDate;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
}
