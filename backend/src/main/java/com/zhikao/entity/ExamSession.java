package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 考试记录（考生考试会话）实体
 */
@Data
@TableName("exam_session")
public class ExamSession implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 试卷ID */
    private Long paperId;

    /** 考生ID */
    private Long userId;

    /** 开始时间 */
    private Date startTime;

    /** 提交时间 */
    private Date submitTime;

    /** 实际用时(秒) */
    private Integer durationUsed;

    /** 状态：0进行中 1已提交 2已超时 3已批阅 */
    private Integer status;

    /** IP地址 */
    private String ipAddress;

    /** 设备指纹 */
    private String deviceFingerprint;

    /** 切屏次数 */
    private Integer screenSwitchCount;

    /** 作弊标记：0正常 1疑似 2确认作弊 */
    private Integer cheatFlag;

    /** 总分 */
    private java.math.BigDecimal totalScore;

    /** 客观题得分 */
    private java.math.BigDecimal objectiveScore;

    /** 主观题得分 */
    private java.math.BigDecimal subjectiveScore;

    /** 所属班级ID（考试时从class_member获取） */
    private Long classId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

}
