package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 排行榜历史记录实体
 */
@Data
@TableName("leaderboard_record")
public class LeaderboardRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 排行榜ID */
    private Long leaderboardId;

    /** 用户ID */
    private Long userId;

    /** 队伍ID */
    private Long teamId;

    /** 排名 */
    private Integer rankNum;

    /** 分数 */
    private Integer score;

    /** 奖励状态：0未发放 1已发放 */
    private Integer rewardStatus;

    /** 赛季标识 */
    private String seasonKey;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
}
