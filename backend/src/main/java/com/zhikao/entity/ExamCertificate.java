package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 考试证书实体
 */
@Data
@TableName("exam_certificate")
public class ExamCertificate implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 考试记录ID */
    private Long sessionId;

    /** 用户ID */
    private Long userId;

    /** 试卷ID */
    private Long paperId;

    /** 证书编号 */
    private String certNo;

    /** 证书文件URL */
    private String certUrl;

    /** 颁发时间 */
    private Date issueTime;
}
