package com.zhikao.controller;

import com.zhikao.common.PageResult;
import com.zhikao.common.Result;
import com.zhikao.entity.ClassEntity;
import com.zhikao.entity.ClassMember;
import com.zhikao.service.ClassService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 班级控制器
 */
@RestController
@RequestMapping("/class")
public class ClassController {

    @Autowired
    private ClassService classService;

    /**
     * 创建班级（管理员/教师）
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TEACHER')")
    @PostMapping("/create")
    public Result<ClassEntity> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        ClassEntity classEntity = new ClassEntity();
        classEntity.setClassName((String) body.get("className"));
        classEntity.setSchool((String) body.get("school"));
        classEntity.setGrade((String) body.get("grade"));
        classEntity.setDescription((String) body.get("description"));
        if (body.get("maxStudents") != null) {
            classEntity.setMaxStudents(Integer.valueOf(body.get("maxStudents").toString()));
        }
        ClassEntity created = classService.createClass(classEntity, userId);
        return Result.success("班级创建成功，口令：" + created.getClassCode(), created);
    }

    /**
     * 通过口令加入班级
     */
    @PostMapping("/join")
    public Result<?> join(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        String classCode = body.get("classCode");
        if (classCode == null || classCode.trim().isEmpty()) {
            return Result.badRequest("口令不能为空");
        }
        classService.joinClass(classCode.trim(), userId, role);
        return Result.success("加入班级成功");
    }

    /**
     * 学生退出当前班级
     */
    @DeleteMapping("/leave")
    public Result<?> leave(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        classService.leaveClass(userId);
        return Result.success("已退出班级");
    }

    /**
     * 获取当前用户已加入的班级列表
     */
    @GetMapping("/my")
    public Result<List<ClassEntity>> my(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<ClassEntity> classes = classService.listMyClasses(userId);
        return Result.success(classes);
    }

    /**
     * 获取班级成员列表
     */
    @GetMapping("/members/{classId}")
    public Result<List<ClassMember>> members(@PathVariable Long classId) {
        List<ClassMember> members = classService.listMembers(classId);
        return Result.success(members);
    }

    /**
     * 解散班级（仅创建者/管理员）
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TEACHER')")
    @DeleteMapping("/{classId}")
    public Result<?> dismiss(@PathVariable Long classId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        classService.dismissClass(classId, userId);
        return Result.success("班级已解散");
    }

    /**
     * 班级列表（管理员看全部，教师只看自己创建的班级）
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TEACHER')")
    @GetMapping("/list")
    public Result<PageResult<ClassEntity>> listAll(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long teacherId,
            HttpServletRequest request) {
        // 教师只能查看自己的班级，管理员可选择筛选
        String role = (String) request.getAttribute("role");
        if ("TEACHER".equals(role)) {
            teacherId = (Long) request.getAttribute("userId");
        }
        var page = classService.listAllClasses(current, size, keyword, teacherId);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), page.getSize(), page.getCurrent()));
    }

    /**
     * 教师移除班级学生
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TEACHER')")
    @DeleteMapping("/members/{classId}/user/{userId}")
    public Result<?> removeStudent(@PathVariable Long classId,
                                    @PathVariable Long userId,
                                    HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        classService.removeStudent(classId, userId, teacherId);
        return Result.success("已移除学生");
    }

    /**
     * 获取有班级的教师列表（供管理员筛选，教师仅返回自己）
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TEACHER')")
    @GetMapping("/teachers")
    public Result<List<Map<String, Object>>> listTeachers(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if ("TEACHER".equals(role)) {
            // 教师不需要教师筛选列表
            return Result.success(List.of());
        }
        return Result.success(classService.listTeachersWithClasses());
    }

    /**
     * 编辑班级信息（仅创建者/管理员）
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TEACHER')")
    @PutMapping("/{classId}")
    public Result<?> update(@PathVariable Long classId,
                            @RequestBody Map<String, Object> body,
                            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        ClassEntity updates = new ClassEntity();
        if (body.containsKey("className")) updates.setClassName((String) body.get("className"));
        if (body.containsKey("school")) updates.setSchool((String) body.get("school"));
        if (body.containsKey("grade")) updates.setGrade((String) body.get("grade"));
        if (body.containsKey("description")) updates.setDescription((String) body.get("description"));
        if (body.get("maxStudents") != null) updates.setMaxStudents(Integer.valueOf(body.get("maxStudents").toString()));
        classService.updateClass(classId, updates, userId);
        return Result.success("保存成功");
    }

    /**
     * 重新生成班级口令（仅创建者/管理员）
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TEACHER')")
    @PutMapping("/{classId}/regenerate-code")
    public Result<String> regenerateCode(@PathVariable Long classId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String newCode = classService.regenerateClassCode(classId, userId);
        return Result.success("新口令已生成", newCode);
    }
}
