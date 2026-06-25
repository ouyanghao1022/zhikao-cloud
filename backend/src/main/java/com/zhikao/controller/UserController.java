package com.zhikao.controller;

import com.zhikao.common.PageResult;
import com.zhikao.common.Result;
import com.zhikao.entity.*;
import com.zhikao.service.UserService;
import com.zhikao.mapper.ExamPaperMapper;
import com.zhikao.mapper.ExamSessionMapper;
import com.zhikao.mapper.RoleMapper;
import com.zhikao.mapper.UserRoleMapper;
import com.zhikao.mapper.ExamAnswerMapper;
import com.zhikao.mapper.FavoriteItemMapper;
import com.zhikao.mapper.WrongNotebookMapper;
import com.zhikao.mapper.ClassMemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器 - 个人资料修改、密码修改
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ExamPaperMapper examPaperMapper;

    @Autowired
    private ExamSessionMapper examSessionMapper;

    @Autowired
    private com.zhikao.service.QuestionService questionService;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private ExamAnswerMapper examAnswerMapper;

    @Autowired
    private FavoriteItemMapper favoriteItemMapper;

    @Autowired
    private WrongNotebookMapper wrongNotebookMapper;

    @Autowired
    private ClassMemberMapper classMemberMapper;

    @Autowired
    private com.zhikao.service.RoleService roleService;

    /**
     * 更新个人资料
     */
    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody Map<String, Object> updates, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized("未登录");
        }

        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        if (updates.containsKey("nickname")) user.setNickname((String) updates.get("nickname"));
        if (updates.containsKey("email")) user.setEmail((String) updates.get("email"));
        if (updates.containsKey("phone")) user.setPhone((String) updates.get("phone"));
        if (updates.containsKey("gender")) user.setGender((Integer) updates.get("gender"));
        if (updates.containsKey("school")) user.setSchool((String) updates.get("school"));
        // 年级修改规则：学生未加入班级时允许自由修改；已加入班级则年级由班级决定
        String role = (String) request.getAttribute("role");
        if (updates.containsKey("grade")) {
            if (!"STUDENT".equals(role)) {
                // 教师/管理员始终可修改
                user.setGrade((String) updates.get("grade"));
            } else {
                // 学生：仅未加入任何班级时允许修改
                com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ClassMember> checkWrapper =
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
                checkWrapper.eq(ClassMember::getUserId, userId).eq(ClassMember::getStatus, 1);
                Long memberCount = classMemberMapper.selectCount(checkWrapper);
                if (memberCount == 0) {
                    // 未加入任何班级，允许自由修改年级
                    user.setGrade((String) updates.get("grade"));
                }
                // 已加入班级 → 忽略 grade，年级由班级决定
            }
        }
        if (updates.containsKey("signature")) user.setSignature((String) updates.get("signature"));
        if (updates.containsKey("avatar")) user.setAvatar((String) updates.get("avatar"));

        userService.updateById(user);
        return Result.success("保存成功");
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<?> changePassword(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized("未登录");
        }

        String oldPassword = (String) body.get("oldPassword");
        String newPassword = (String) body.get("newPassword");
        String confirmPassword = (String) body.get("confirmPassword");

        if (StrUtil.isBlank(oldPassword) || StrUtil.isBlank(newPassword) || StrUtil.isBlank(confirmPassword)) {
            return Result.badRequest("请填写完整信息");
        }
        if (!newPassword.equals(confirmPassword)) {
            return Result.badRequest("两次输入的新密码不一致");
        }
        if (newPassword.length() < 6) {
            return Result.badRequest("新密码长度不能少于6位");
        }

        User user = userService.getById(userId);
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            return Result.badRequest("原密码错误");
        }

        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        userService.updateById(user);
        return Result.success("密码修改成功，请重新登录");
    }

    // ==================== 管理员接口 ====================

    /**
     * 用户列表（管理员）
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/list")
    public Result<PageResult<User>> listUsers(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getNickname, keyword));
        }
        if (StrUtil.isNotBlank(role)) {
            wrapper.apply("EXISTS (SELECT 1 FROM sys_user_role ur JOIN sys_role r ON ur.role_id = r.id WHERE ur.user_id = sys_user.id AND r.role_code = {0})", role);
        }
        // 按角色优先级排序：管理员 > 教师 > 学生，然后按创建时间倒序
        wrapper.last("ORDER BY CASE WHEN EXISTS(SELECT 1 FROM sys_user_role ur JOIN sys_role r ON ur.role_id=r.id WHERE ur.user_id=sys_user.id AND r.role_code='SUPER_ADMIN') THEN 1 WHEN EXISTS(SELECT 1 FROM sys_user_role ur JOIN sys_role r ON ur.role_id=r.id WHERE ur.user_id=sys_user.id AND r.role_code='TEACHER') THEN 2 ELSE 3 END, created_at DESC");

        com.baomidou.mybatisplus.core.metadata.IPage<User> page = userService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size),
                wrapper
        );

        // 填充每个用户的角色信息
        for (User user : page.getRecords()) {
            user.setRoles(roleService.getRolesByUserId(user.getId()));
        }

        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), page.getSize(), page.getCurrent()));
    }

    /**
     * 删除用户（管理员）
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<?> deleteUser(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success("已删除");
    }

    /**
     * 仪表盘统计数据（管理员/教师）
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TEACHER')")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> data = new HashMap<>();
        data.put("userCount", userService.count());
        data.put("examCount", examPaperMapper.selectCount(null));
        data.put("questionCount", questionService.count());
        data.put("sessionCount", examSessionMapper.selectCount(null));
        return Result.success(data);
    }

    /**
     * 学生个人考试统计（任何已登录用户可用）
     */
    @GetMapping("/my-stats")
    public Result<Map<String, Object>> myStats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized("未登录");
        }

        LambdaQueryWrapper<ExamSession> sessionWrapper = new LambdaQueryWrapper<>();
        sessionWrapper.eq(ExamSession::getUserId, userId)
                       .ge(ExamSession::getStatus, 1);  // 已提交及以上（含超时、已批阅）

        java.util.List<ExamSession> sessions = examSessionMapper.selectList(sessionWrapper);
        int examCount = sessions.size();
        double avgScore = sessions.stream()
                .mapToDouble(s -> s.getTotalScore() != null ? s.getTotalScore().doubleValue() : 0)
                .average().orElse(0);
        double maxScore = sessions.stream()
                .mapToDouble(s -> s.getTotalScore() != null ? s.getTotalScore().doubleValue() : 0)
                .max().orElse(0);

        // 总做题量 & 正确数 & 错题数
        long totalQuestions = 0;
        long correctCount = 0;
        long wrongCount = 0;
        if (examCount > 0) {
            LambdaQueryWrapper<ExamAnswer> totalWrapper = new LambdaQueryWrapper<>();
            totalWrapper.inSql(ExamAnswer::getSessionId,
                            "SELECT id FROM exam_session WHERE user_id = " + userId + " AND status IN (1,3)");
            totalQuestions = examAnswerMapper.selectCount(totalWrapper);

            LambdaQueryWrapper<ExamAnswer> correctWrapper = new LambdaQueryWrapper<>();
            correctWrapper.eq(ExamAnswer::getIsCorrect, 1)
                         .inSql(ExamAnswer::getSessionId,
                            "SELECT id FROM exam_session WHERE user_id = " + userId + " AND status IN (1,3)");
            correctCount = examAnswerMapper.selectCount(correctWrapper);

            LambdaQueryWrapper<ExamAnswer> wrongWrapper = new LambdaQueryWrapper<>();
            wrongWrapper.eq(ExamAnswer::getIsCorrect, 0)
                         .inSql(ExamAnswer::getSessionId,
                            "SELECT id FROM exam_session WHERE user_id = " + userId + " AND status IN (1,3)");
            wrongCount = examAnswerMapper.selectCount(wrongWrapper);
        }

        // 正确率
        double correctRate = totalQuestions > 0
                ? Math.round((double) correctCount / totalQuestions * 1000.0) / 10.0
                : 0;

        // 错题本条目数
        LambdaQueryWrapper<WrongNotebook> bookWrapper = new LambdaQueryWrapper<>();
        bookWrapper.eq(WrongNotebook::getUserId, userId);
        long wrongBookCount = wrongNotebookMapper.selectCount(bookWrapper);

        // 收藏数
        LambdaQueryWrapper<FavoriteItem> favWrapper = new LambdaQueryWrapper<>();
        favWrapper.eq(FavoriteItem::getUserId, userId);
        long favoriteCount = favoriteItemMapper.selectCount(favWrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("examCount", examCount);
        data.put("avgScore", Math.round(avgScore * 100.0) / 100.0);
        data.put("maxScore", maxScore);
        data.put("totalQuestions", totalQuestions);
        data.put("correctCount", correctCount);
        data.put("correctRate", correctRate);
        data.put("wrongCount", wrongCount);
        data.put("wrongBookCount", wrongBookCount);
        data.put("favoriteCount", favoriteCount);
        return Result.success(data);
    }

    /**
     * 个人中心概览数据（按角色返回不同统计数据）
     * 学生 → 学习统计 / 教师 → 教学统计 / 管理员 → 系统概览
     */
    @GetMapping("/my-dashboard")
    public Result<Map<String, Object>> myDashboard(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        if (userId == null) {
            return Result.unauthorized("未登录");
        }

        Map<String, Object> data = new HashMap<>();

        if ("STUDENT".equals(role)) {
            // 学生：复用 myStats 逻辑，返回简化的学习概览
            return myStats(request);
        } else if ("TEACHER".equals(role)) {
            // 教师：我的班级数 + 学生总数 + 试卷数
            LambdaQueryWrapper<ClassMember> memberWrapper = new LambdaQueryWrapper<>();
            memberWrapper.eq(ClassMember::getUserId, userId).eq(ClassMember::getStatus, 1);
            java.util.List<ClassMember> myMembers = classMemberMapper.selectList(memberWrapper);
            long classCount = myMembers.size();

            long studentCount = 0;
            if (classCount > 0) {
                java.util.List<Long> classIds = myMembers.stream().map(ClassMember::getClassId).distinct().toList();
                LambdaQueryWrapper<ClassMember> studentWrapper = new LambdaQueryWrapper<>();
                studentWrapper.in(ClassMember::getClassId, classIds)
                        .eq(ClassMember::getRole, 0).eq(ClassMember::getStatus, 1);
                studentCount = classMemberMapper.selectCount(studentWrapper);
            }

            LambdaQueryWrapper<ExamPaper> paperWrapper = new LambdaQueryWrapper<>();
            paperWrapper.eq(ExamPaper::getCreatorId, userId);
            long examPaperCount = examPaperMapper.selectCount(paperWrapper);

            // 老师关联的考试记录数
            LambdaQueryWrapper<ExamSession> sessionWrapper = new LambdaQueryWrapper<>();
            sessionWrapper.inSql(ExamSession::getPaperId,
                    "SELECT id FROM exam_paper WHERE creator_id = " + userId);
            long examRecordCount = examSessionMapper.selectCount(sessionWrapper);

            data.put("classCount", classCount);
            data.put("studentCount", studentCount);
            data.put("examPaperCount", examPaperCount);
            data.put("examCount", examRecordCount);
        } else {
            // 管理员：系统概览
            data.put("userCount", userService.count());
            data.put("examCount", examPaperMapper.selectCount(null));
            data.put("questionCount", questionService.count());
            data.put("sessionCount", examSessionMapper.selectCount(null));

            // 班级数
            LambdaQueryWrapper<ClassMember> statsWrapper = new LambdaQueryWrapper<>();
            statsWrapper.eq(ClassMember::getStatus, 1);
            statsWrapper.select(ClassMember::getClassId);
            statsWrapper.groupBy(ClassMember::getClassId);
            java.util.List<ClassMember> distinctClasses = classMemberMapper.selectList(statsWrapper);
            data.put("classCount", (long) distinctClasses.size());
        }

        return Result.success(data);
    }

    /**
     * 管理员重置用户密码
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}/reset-password")
    public Result<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        String newPassword = body.getOrDefault("newPassword", "123456");
        if (newPassword.length() < 6) {
            return Result.badRequest("密码不能少于6位");
        }
        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        userService.updateById(user);
        return Result.success("密码已重置为: " + newPassword);
    }

    /**
     * 管理员更新用户角色
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    @PutMapping("/{id}/role")
    public Result<?> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body,
                                 HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        String newRole = body.get("role");
        if (StrUtil.isBlank(newRole)) return Result.badRequest("角色不能为空");

        // 禁止修改自己的角色
        if (currentUserId.equals(id)) {
            return Result.error("不能修改自己的角色");
        }

        // 查找目标角色
        Role targetRole = roleMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Role>()
                .eq(Role::getRoleCode, newRole)
        );
        if (targetRole == null) return Result.error("角色不存在: " + newRole);

        // 如果要移除管理员，检查是否还有至少一个管理员
        if (!"SUPER_ADMIN".equals(newRole)) {
            // 检查目标用户当前是否是管理员
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRole> currentRoleWrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            currentRoleWrapper.eq(UserRole::getUserId, id);
            java.util.List<UserRole> currentRoles = userRoleMapper.selectList(currentRoleWrapper);
            boolean isCurrentlyAdmin = currentRoles.stream().anyMatch(ur -> {
                Role r = roleMapper.selectById(ur.getRoleId());
                return r != null && "SUPER_ADMIN".equals(r.getRoleCode());
            });

            if (isCurrentlyAdmin) {
                // 统计还有多少个管理员
                com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRole> allAdminWrapper =
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
                allAdminWrapper.inSql(UserRole::getRoleId,
                        "SELECT id FROM sys_role WHERE role_code = 'SUPER_ADMIN'");
                long adminCount = userRoleMapper.selectCount(allAdminWrapper);
                if (adminCount <= 1) {
                    return Result.error("系统至少需要保留一个管理员");
                }
            }
        }

        // 清除旧角色
        userRoleMapper.delete(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, id)
        );

        // 分配新角色
        UserRole ur = new UserRole();
        ur.setUserId(id);
        ur.setRoleId(targetRole.getId());
        userRoleMapper.insert(ur);

        return Result.success("角色已更新为: " + newRole);
    }
}
