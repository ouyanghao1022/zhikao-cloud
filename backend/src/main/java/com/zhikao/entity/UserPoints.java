package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("user_points")
public class UserPoints implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer totalPoints;      // 总积分
    private Integer currentLevel;     // 当前等级 1-7
    private Integer experience;        // 当前等级经验值
    private Integer checkinStreak;     // 连续签到天数
    private Date lastCheckinDate;      // 最后签到日期
    private Date createdAt;
    private Date updatedAt;
}
