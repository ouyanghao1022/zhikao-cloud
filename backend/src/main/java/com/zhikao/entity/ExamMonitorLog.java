package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 考试监控日志实体
 */
@Data
@TableName("exam_monitor_log")
public class ExamMonitorLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 考试记录ID */
    private Long sessionId;

    /** 事件类型：1切屏 2拍照 3IP变更 4设备变更 5异常答题速度 */
    private Integer eventType;

    /** 事件详情JSON */
    private String eventDetail;

    /** 风险等级：0低 1中 2高 */
    private Integer riskLevel;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
}
