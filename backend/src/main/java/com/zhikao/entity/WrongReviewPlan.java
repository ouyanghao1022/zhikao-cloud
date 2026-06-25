package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 错题复习计划实体
 */
@Data
@TableName("wrong_review_plan")
public class WrongReviewPlan implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 复习日期 */
    private Date reviewDate;

    /** 当日应复习错题数 */
    private Integer reviewCount;

    /** 已完成复习数 */
    private Integer completedCount;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
}
