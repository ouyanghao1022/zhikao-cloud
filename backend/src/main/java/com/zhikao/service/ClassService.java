package com.zhikao.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhikao.entity.ClassEntity;
import com.zhikao.entity.ClassMember;

import java.util.List;
import java.util.Map;

/**
 * 班级Service接口
 */
public interface ClassService {

    /** 创建班级（教师/管理员），返回含口令的班级对象 */
    ClassEntity createClass(ClassEntity classEntity, Long userId);

    /** 通过口令加入班级，role 为用户全局角色（STUDENT/TEACHER/SUPER_ADMIN） */
    void joinClass(String classCode, Long userId, String role);

    /** 学生退出当前班级 */
    void leaveClass(Long userId);

    /** 获取当前用户已加入的班级列表 */
    List<ClassEntity> listMyClasses(Long userId);

    /** 获取班级成员列表 */
    List<ClassMember> listMembers(Long classId);

    /** 解散班级（仅创建者/管理员） */
    void dismissClass(Long classId, Long userId);

    /** 管理员查看所有班级（支持按教师筛选） */
    IPage<ClassEntity> listAllClasses(Integer current, Integer size, String keyword, Long teacherId);

    /** 获取有班级的教师列表 */
    List<Map<String, Object>> listTeachersWithClasses();

    /** 教师移除班级中的学生 */
    void removeStudent(Long classId, Long studentId, Long teacherId);

    /** 编辑班级信息（仅创建者/管理员） */
    void updateClass(Long classId, ClassEntity updates, Long userId);

    /** 重新生成班级口令（仅创建者/管理员） */
    String regenerateClassCode(Long classId, Long userId);
}
