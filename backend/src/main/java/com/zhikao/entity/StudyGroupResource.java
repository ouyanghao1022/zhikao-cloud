package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 学习小组资源实体
 */
@Data
@TableName("study_group_resource")
public class StudyGroupResource implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private Long uploaderId;

    private String fileName;

    private String fileUrl;

    private Long fileSize;

    private Integer downloadCount;

    private Date createdAt;
}
