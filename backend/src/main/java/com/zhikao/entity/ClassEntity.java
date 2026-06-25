package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 班级实体
 */
@Data
@TableName("sys_class")
public class ClassEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 班级名称 */
    private String className;

    /** 班级口令（6位随机码） */
    private String classCode;

    /** 创建教师ID */
    private Long teacherId;

    /** 学校 */
    private String school;

    /** 年级 */
    private String grade;

    /** 班级描述 */
    private String description;

    /** 最大学生数 */
    private Integer maxStudents;

    /** 当前学生数 */
    private Integer studentCount;

    /** 状态：0已解散 1正常 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    /** 教师昵称（非数据库字段） */
    @TableField(exist = false)
    private String teacherName;
}
