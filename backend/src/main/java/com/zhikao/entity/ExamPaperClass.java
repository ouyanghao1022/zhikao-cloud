package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 考试-班级绑定实体
 */
@Data
@TableName("exam_paper_class")
public class ExamPaperClass implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long paperId;

    private Long classId;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
}
