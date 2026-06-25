package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户等级配置实体
 */
@Data
@TableName("user_level")
public class UserLevel implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 等级名称：青铜/白银/黄金/铂金/钻石/星耀/王者 */
    private String levelName;

    /** 等级编码：1-7 */
    private Integer levelCode;

    /** 最小经验值 */
    private Integer minExperience;

    /** 最大经验值 */
    private Integer maxExperience;

    /** 等级图标 */
    private String icon;

    /** 等级特权JSON */
    private String privileges;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
}
