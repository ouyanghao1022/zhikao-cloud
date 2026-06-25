package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhikao.entity.*;
import com.zhikao.mapper.*;
import com.zhikao.service.ClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import cn.hutool.core.util.StrUtil;

/**
 * 班级Service实现类
 */
@Service
public class ClassServiceImpl implements ClassService {

    @Autowired
    private ClassMapper classMapper;
    @Autowired
    private ClassMemberMapper classMemberMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();

    @Override
    @Transactional
    public ClassEntity createClass(ClassEntity classEntity, Long userId) {
        // 生成唯一6位口令
        String classCode;
        do {
            classCode = generateClassCode();
        } while (isClassCodeExists(classCode));

        classEntity.setTeacherId(userId);
        classEntity.setClassCode(classCode);
        classEntity.setStudentCount(0);
        classEntity.setStatus(1);
        if (classEntity.getMaxStudents() == null) {
            classEntity.setMaxStudents(100);
        }
        classEntity.setCreatedAt(new Date());
        classEntity.setUpdatedAt(new Date());
        classMapper.insert(classEntity);

        // 创建人自动成为班级教师
        ClassMember member = new ClassMember();
        member.setClassId(classEntity.getId());
        member.setUserId(userId);
        member.setRole(1); // 教师(创建者)
        member.setJoinTime(new Date());
        member.setStatus(1);
        member.setCreatedAt(new Date());
        member.setUpdatedAt(new Date());
        classMemberMapper.insert(member);

        return classEntity;
    }

    @Override
    @Transactional
    public void joinClass(String classCode, Long userId, String role) {
        // 查找班级
        LambdaQueryWrapper<ClassEntity> classWrapper = new LambdaQueryWrapper<>();
        classWrapper.eq(ClassEntity::getClassCode, classCode.toUpperCase())
                .eq(ClassEntity::getStatus, 1);
        ClassEntity classEntity = classMapper.selectOne(classWrapper);
        if (classEntity == null) {
            throw new RuntimeException("班级不存在或已解散，请检查口令是否正确");
        }

        // 检查是否已是成员
        LambdaQueryWrapper<ClassMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(ClassMember::getClassId, classEntity.getId())
                .eq(ClassMember::getUserId, userId)
                .eq(ClassMember::getStatus, 1);
        if (classMemberMapper.selectCount(memberWrapper) > 0) {
            throw new RuntimeException("您已是该班级成员");
        }

        // 判断用户角色决定班级内身份
        boolean isTeacherRole = "TEACHER".equals(role) || "SUPER_ADMIN".equals(role);
        int memberRole = isTeacherRole ? 1 : 0; // 1=教师 0=学生

        // 学生只能加入一个班级
        if (!isTeacherRole) {
            LambdaQueryWrapper<ClassMember> studentCheck = new LambdaQueryWrapper<>();
            studentCheck.eq(ClassMember::getUserId, userId)
                    .eq(ClassMember::getStatus, 1);
            if (classMemberMapper.selectCount(studentCheck) > 0) {
                throw new RuntimeException("一个学生只能加入一个班级，请先退出当前班级");
            }
        }

        // 教师加入时检查年级匹配（学生需要，教师不需要）
        if (!isTeacherRole && classEntity.getGrade() != null && !classEntity.getGrade().isEmpty()) {
            User student = userMapper.selectById(userId);
            if (student != null && student.getGrade() != null && !student.getGrade().isEmpty()) {
                if (!classEntity.getGrade().equals(student.getGrade())) {
                    throw new RuntimeException("您的年级（" + student.getGrade() + "）与该班级年级（" + classEntity.getGrade() + "）不匹配");
                }
            }
        }

        // 检查人数上限
        if (classEntity.getStudentCount() >= classEntity.getMaxStudents()) {
            throw new RuntimeException("班级已满员");
        }

        // 加入班级
        ClassMember member = new ClassMember();
        member.setClassId(classEntity.getId());
        member.setUserId(userId);
        member.setRole(memberRole);
        member.setJoinTime(new Date());
        member.setStatus(1);
        member.setCreatedAt(new Date());
        member.setUpdatedAt(new Date());
        classMemberMapper.insert(member);

        // 更新班级人数
        classEntity.setStudentCount(classEntity.getStudentCount() + 1);
        classMapper.updateById(classEntity);

        // 加入班级后，同步学生年级到班级年级（保证个人中心"我的班级"与"基本信息"年级一致）
        if (!isTeacherRole && classEntity.getGrade() != null && !classEntity.getGrade().isEmpty()) {
            User student = userMapper.selectById(userId);
            if (student != null && (student.getGrade() == null || student.getGrade().isEmpty()
                    || !classEntity.getGrade().equals(student.getGrade()))) {
                student.setGrade(classEntity.getGrade());
                student.setUpdatedAt(new Date());
                userMapper.updateById(student);
            }
        }
    }

    @Override
    @Transactional
    public void leaveClass(Long userId) {
        // 查找学生当前激活的班级成员记录
        LambdaQueryWrapper<ClassMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(ClassMember::getUserId, userId)
                .eq(ClassMember::getStatus, 1)
                .eq(ClassMember::getRole, 0)  // 只允许学生退出
                .orderByDesc(ClassMember::getId)
                .last("LIMIT 1");
        ClassMember member = classMemberMapper.selectOne(memberWrapper);
        if (member == null) {
            throw new RuntimeException("您当前没有加入任何班级");
        }

        // 更新成员状态为已退出
        member.setStatus(0);
        member.setUpdatedAt(new Date());
        classMemberMapper.updateById(member);

        // 更新班级人数
        ClassEntity classEntity = classMapper.selectById(member.getClassId());
        if (classEntity != null && classEntity.getStudentCount() > 0) {
            classEntity.setStudentCount(classEntity.getStudentCount() - 1);
            classMapper.updateById(classEntity);
        }
    }

    @Override
    public List<ClassEntity> listMyClasses(Long userId) {
        // 查询用户作为成员的所有班级
        LambdaQueryWrapper<ClassMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(ClassMember::getUserId, userId)
                .eq(ClassMember::getStatus, 1);
        List<ClassMember> members = classMemberMapper.selectList(memberWrapper);

        if (members.isEmpty()) {
            return List.of();
        }

        List<Long> classIds = members.stream().map(ClassMember::getClassId).toList();
        LambdaQueryWrapper<ClassEntity> classWrapper = new LambdaQueryWrapper<>();
        classWrapper.in(ClassEntity::getId, classIds)
                .eq(ClassEntity::getStatus, 1)
                .orderByDesc(ClassEntity::getCreatedAt);
        return classMapper.selectList(classWrapper);
    }

    @Override
    public List<ClassMember> listMembers(Long classId) {
        LambdaQueryWrapper<ClassMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getStatus, 1)
                .orderByAsc(ClassMember::getRole)
                .orderByAsc(ClassMember::getJoinTime);
        List<ClassMember> members = classMemberMapper.selectList(wrapper);
        
        // 填充用户信息（昵称、用户名）
        if (!members.isEmpty()) {
            List<Long> userIds = members.stream().map(ClassMember::getUserId).distinct().toList();
            List<User> users = userMapper.selectBatchIds(userIds);
            Map<Long, User> userMap = new HashMap<>();
            for (User u : users) {
                userMap.put(u.getId(), u);
            }
            for (ClassMember m : members) {
                User u = userMap.get(m.getUserId());
                if (u != null) {
                    m.setUsername(u.getUsername());
                    m.setNickname(u.getNickname());
                }
            }
        }
        return members;
    }

    @Override
    @Transactional
    public void dismissClass(Long classId, Long userId) {
        ClassEntity classEntity = classMapper.selectById(classId);
        if (classEntity == null || classEntity.getStatus() == 0) {
            throw new RuntimeException("班级不存在或已解散");
        }

        // 创建者可以直接解散
        if (classEntity.getTeacherId().equals(userId)) {
            executeDismiss(classEntity, classId);
            return;
        }

        // 非创建者：检查是否为超级管理员
        if (!isUserAdmin(userId)) {
            throw new RuntimeException("只有班级创建者或管理员才能解散班级");
        }

        executeDismiss(classEntity, classId);
    }

    /**
     * 检查用户是否拥有管理员权限（SUPER_ADMIN 角色）
     */
    private boolean isUserAdmin(Long userId) {
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, userId);
        List<UserRole> userRoles = userRoleMapper.selectList(wrapper);
        if (userRoles.isEmpty()) return false;

        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).toList();
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        return roles.stream().anyMatch(r -> "SUPER_ADMIN".equals(r.getRoleCode()));
    }

    /**
     * 执行班级解散操作
     */
    private void executeDismiss(ClassEntity classEntity, Long classId) {
        classEntity.setStatus(0);
        classMapper.updateById(classEntity);

        LambdaQueryWrapper<ClassMember> allMembers = new LambdaQueryWrapper<>();
        allMembers.eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getStatus, 1);
        ClassMember updateMember = new ClassMember();
        updateMember.setStatus(0);
        classMemberMapper.update(updateMember, allMembers);
    }

    @Override
    public IPage<ClassEntity> listAllClasses(Integer current, Integer size, String keyword, Long teacherId) {
        LambdaQueryWrapper<ClassEntity> wrapper = new LambdaQueryWrapper<>();
        // 只显示未解散的班级
        wrapper.eq(ClassEntity::getStatus, 1);
        if (cn.hutool.core.util.StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(ClassEntity::getClassName, keyword)
                    .or().like(ClassEntity::getClassCode, keyword));
        }
        if (teacherId != null) {
            wrapper.eq(ClassEntity::getTeacherId, teacherId);
        }
        wrapper.orderByDesc(ClassEntity::getCreatedAt);
        IPage<ClassEntity> page = classMapper.selectPage(new Page<>(current, size), wrapper);

        // 填充教师名称
        List<ClassEntity> records = page.getRecords();
        if (!records.isEmpty()) {
            List<Long> teacherIds = records.stream().map(ClassEntity::getTeacherId).distinct().toList();
            List<User> teachers = userMapper.selectBatchIds(teacherIds);
            Map<Long, User> teacherMap = new HashMap<>();
            for (User t : teachers) teacherMap.put(t.getId(), t);
            for (ClassEntity c : records) {
                User t = teacherMap.get(c.getTeacherId());
                if (t != null) c.setTeacherName(t.getNickname());
            }
        }
        return page;
    }

    @Override
    public List<Map<String, Object>> listTeachersWithClasses() {
        // 查询所有有班级的教师ID（去重）
        LambdaQueryWrapper<ClassEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassEntity::getStatus, 1)
                .select(ClassEntity::getTeacherId)
                .groupBy(ClassEntity::getTeacherId);
        List<ClassEntity> classes = classMapper.selectList(wrapper);

        if (classes.isEmpty()) return List.of();

        List<Long> teacherIds = classes.stream()
                .map(ClassEntity::getTeacherId).distinct().toList();
        List<User> users = userMapper.selectBatchIds(teacherIds);

        // 查询每个教师的班级数量
        return users.stream().map(u -> {
            long classCount = classes.stream()
                    .filter(c -> c.getTeacherId().equals(u.getId())).count();
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("name", u.getNickname() != null && !u.getNickname().isEmpty()
                    ? u.getNickname() : u.getUsername());
            map.put("username", u.getUsername());
            map.put("avatar", u.getAvatar());
            map.put("classCount", classCount);
            return map;
        }).toList();
    }

    /**
     * 教师移除班级中的学生
     */
    @Override
    @Transactional
    public void removeStudent(Long classId, Long studentId, Long teacherId) {
        // 校验班级存在且属于该教师
        ClassEntity classEntity = classMapper.selectById(classId);
        if (classEntity == null || classEntity.getStatus() != 1) {
            throw new RuntimeException("班级不存在或已解散");
        }
        if (!teacherId.equals(classEntity.getTeacherId())) {
            throw new RuntimeException("您不是该班级的创建者，无权移除学生");
        }

        // 查找学生在班级中的成员记录
        LambdaQueryWrapper<ClassMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getUserId, studentId)
                .eq(ClassMember::getStatus, 1)
                .last("LIMIT 1");
        ClassMember member = classMemberMapper.selectOne(wrapper);
        if (member == null) {
            throw new RuntimeException("该学生不在班级中");
        }
        if (member.getRole() == 1) {
            throw new RuntimeException("不能移除教师");
        }

        // 标记为已退出
        member.setStatus(0);
        member.setUpdatedAt(new Date());
        classMemberMapper.updateById(member);

        // 更新班级人数
        classEntity.setStudentCount(Math.max(0, classEntity.getStudentCount() - 1));
        classMapper.updateById(classEntity);
    }

    /**
     * 生成6位随机口令
     */
    private String generateClassCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    @Override
    @Transactional
    public void updateClass(Long classId, ClassEntity updates, Long userId) {
        ClassEntity classEntity = classMapper.selectById(classId);
        if (classEntity == null || classEntity.getStatus() != 1) {
            throw new RuntimeException("班级不存在或已解散");
        }
        // 仅创建者或管理员可编辑
        User user = userMapper.selectById(userId);
        boolean isAdmin = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> "SUPER_ADMIN".equals(r.getRoleCode()));
        if (!userId.equals(classEntity.getTeacherId()) && !isAdmin) {
            throw new RuntimeException("仅创建教师或管理员可编辑班级");
        }

        if (StrUtil.isNotBlank(updates.getClassName())) classEntity.setClassName(updates.getClassName().trim());
        if (updates.getSchool() != null) classEntity.setSchool(updates.getSchool());
        if (updates.getGrade() != null) classEntity.setGrade(updates.getGrade());
        if (updates.getDescription() != null) classEntity.setDescription(updates.getDescription());
        if (updates.getMaxStudents() != null) {
            int max = updates.getMaxStudents();
            if (max < 5) throw new RuntimeException("最大人数不能少于5");
            if (max > 1000) throw new RuntimeException("最大人数不能超过1000");
            classEntity.setMaxStudents(max);
        }
        classEntity.setUpdatedAt(new Date());
        classMapper.updateById(classEntity);
    }

    @Override
    @Transactional
    public String regenerateClassCode(Long classId, Long userId) {
        ClassEntity classEntity = classMapper.selectById(classId);
        if (classEntity == null || classEntity.getStatus() != 1) {
            throw new RuntimeException("班级不存在或已解散");
        }
        User user = userMapper.selectById(userId);
        boolean isAdmin = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> "SUPER_ADMIN".equals(r.getRoleCode()));
        if (!userId.equals(classEntity.getTeacherId()) && !isAdmin) {
            throw new RuntimeException("仅创建教师或管理员可重新生成口令");
        }

        String newCode;
        int attempts = 0;
        do {
            newCode = generateClassCode();
            attempts++;
            if (attempts > 20) throw new RuntimeException("生成口令失败，请重试");
        } while (isClassCodeExists(newCode) && !newCode.equals(classEntity.getClassCode()));

        classEntity.setClassCode(newCode);
        classEntity.setUpdatedAt(new Date());
        classMapper.updateById(classEntity);
        return newCode;
    }

    /**
     * 检查口令是否已存在
     */
    private boolean isClassCodeExists(String classCode) {
        LambdaQueryWrapper<ClassEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassEntity::getClassCode, classCode);
        return classMapper.selectCount(wrapper) > 0;
    }
}
