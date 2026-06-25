package com.zhikao.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhikao.common.PageResult;
import com.zhikao.common.Result;
import com.zhikao.entity.*;
import com.zhikao.mapper.StudyGroupMapper;
import com.zhikao.mapper.StudyGroupMemberMapper;
import com.zhikao.mapper.StudyGroupTaskMapper;
import com.zhikao.mapper.StudyGroupResourceMapper;
import com.zhikao.service.GroupChatService;
import com.zhikao.service.GroupService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 学习小组控制器
 */
@RestController
@RequestMapping("/group")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private StudyGroupMapper groupMapper;

    @Autowired
    private StudyGroupMemberMapper groupMemberMapper;

    @Autowired
    private StudyGroupTaskMapper groupTaskMapper;

    @Autowired
    private StudyGroupResourceMapper groupResourceMapper;

    @Autowired
    private GroupChatService groupChatService;

    /**
     * 小组列表
     */
    @GetMapping("/list")
    public Result<PageResult<StudyGroup>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer joinType) {
        PageResult<StudyGroup> result = groupService.listGroups(page, size, keyword, joinType);
        return Result.success(result);
    }

    /**
     * 小组详情
     */
    @GetMapping("/{id}")
    public Result<StudyGroup> detail(@PathVariable Long id) {
        StudyGroup group = groupService.getGroup(id);
        if (group == null) {
            return Result.error("小组不存在");
        }
        return Result.success(group);
    }

    /**
     * 创建小组
     */
    @PostMapping
    public Result<StudyGroup> create(@RequestBody StudyGroup group, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        StudyGroup created = groupService.createGroup(group, userId);
        return Result.success("创建成功", created);
    }

    /**
     * 加入小组
     */
    @PostMapping("/{id}/join")
    public Result<?> join(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        groupService.joinGroup(id, userId);
        return Result.success("加入成功");
    }

    /**
     * 退出小组
     */
    @PostMapping("/{id}/leave")
    public Result<?> leave(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        groupService.leaveGroup(id, userId);
        return Result.success("已退出小组");
    }

    /**
     * 解散小组（组长/管理员，有成员数限制）
     * - 小组管理员：仅当小组内除自己外无其他成员时才可解散
     * - 系统管理员：走 /admin/{id} 接口可强制解散
     */
    @DeleteMapping("/{id}")
    public Result<?> dismiss(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        String nickname = (String) request.getAttribute("nickname");
        boolean isSuperAdmin = "SUPER_ADMIN".equals(role);
        // 普通接口：即使是超管走这里也按"组成员"逻辑校验（如超管不是成员则报错）
        // 超管强制解散请走 /group/admin/{id}
        groupService.dismissGroupWithCheck(id, userId, false, nickname);
        return Result.success("已解散小组");
    }

    /**
     * 成员列表
     */
    @GetMapping("/{id}/members")
    public Result<List<StudyGroupMember>> members(@PathVariable Long id) {
        List<StudyGroupMember> members = groupService.listMembers(id);
        return Result.success(members);
    }

    /**
     * 发布任务
     */
    @PostMapping("/{id}/task")
    public Result<StudyGroupTask> createTask(@PathVariable Long id,
                                              @RequestBody StudyGroupTask task,
                                              HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        StudyGroupTask created = groupService.createTask(id, task, userId);
        return Result.success("任务发布成功", created);
    }

    /**
     * 任务列表
     */
    @GetMapping("/{id}/tasks")
    public Result<List<StudyGroupTask>> tasks(@PathVariable Long id,
                                                @RequestParam(required = false) Integer status) {
        List<StudyGroupTask> tasks = groupService.listTasks(id, status);
        return Result.success(tasks);
    }

    /**
     * 资源列表
     */
    @GetMapping("/{id}/resources")
    public Result<List<StudyGroupResource>> resources(@PathVariable Long id) {
        List<StudyGroupResource> resources = groupService.listResources(id);
        return Result.success(resources);
    }

    /**
     * 小组聊天记录（分页，按时间升序）
     */
    @GetMapping("/{groupId}/chat")
    public Result<PageResult<StudyGroupChat>> listChat(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        var result = groupChatService.listMessages(groupId, page, size);
        return Result.success(PageResult.of(result.getRecords(), result.getTotal(),
                result.getSize(), result.getCurrent()));
    }

    /**
     * 发送小组聊天消息
     */
    @PostMapping("/{groupId}/chat")
    public Result<StudyGroupChat> sendChat(@PathVariable Long groupId,
                                           @RequestBody Map<String, Object> body,
                                           HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String content = body.get("content") != null ? body.get("content").toString() : null;
        Integer contentType = body.containsKey("contentType") && body.get("contentType") != null
                ? Integer.valueOf(body.get("contentType").toString()) : 1;
        String fileUrl = body.get("fileUrl") != null ? body.get("fileUrl").toString() : null;
        StudyGroupChat chat = groupChatService.sendMessage(groupId, userId, content, contentType, fileUrl);
        return Result.success("发送成功", chat);
    }

    // ==================== 管理端接口 ====================

    /**
     * 管理端：统计概览
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TEACHER')")
    @GetMapping("/admin/stats")
    public Result<Map<String, Object>> adminStats() {
        Map<String, Object> stats = new HashMap<>();
        long total = groupMapper.selectCount(null);
        long active = groupMapper.selectCount(new LambdaQueryWrapper<StudyGroup>().eq(StudyGroup::getStatus, 1));
        long dismissed = groupMapper.selectCount(new LambdaQueryWrapper<StudyGroup>().eq(StudyGroup::getStatus, 0));
        long members = groupMemberMapper.selectCount(new LambdaQueryWrapper<StudyGroupMember>().eq(StudyGroupMember::getStatus, 1));
        long tasks = groupTaskMapper.selectCount(new LambdaQueryWrapper<StudyGroupTask>().eq(StudyGroupTask::getStatus, 1));
        long resources = groupResourceMapper.selectCount(null);
        stats.put("totalGroups", total);
        stats.put("activeGroups", active);
        stats.put("dismissedGroups", dismissed);
        stats.put("totalMembers", members);
        stats.put("activeTasks", tasks);
        stats.put("totalResources", resources);
        return Result.success(stats);
    }

    /**
     * 管理端：小组列表（含已解散，多条件筛选）
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TEACHER')")
    @GetMapping("/admin/list")
    public Result<PageResult<StudyGroup>> adminList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer joinType,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<StudyGroup> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(StudyGroup::getGroupName, keyword);
        }
        if (joinType != null) {
            wrapper.eq(StudyGroup::getJoinType, joinType);
        }
        if (status != null) {
            wrapper.eq(StudyGroup::getStatus, status);
        }
        wrapper.orderByDesc(StudyGroup::getCreatedAt);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<StudyGroup> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        var result = groupMapper.selectPage(p, wrapper);
        return Result.success(PageResult.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent()));
    }

    /**
     * 管理端：编辑小组信息
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TEACHER')")
    @PutMapping("/admin/{id}")
    public Result<?> adminUpdate(@PathVariable Long id, @RequestBody StudyGroup group) {
        group.setId(id);
        group.setUpdatedAt(new Date());
        groupMapper.updateById(group);
        return Result.success("更新成功");
    }

    /**
     * 管理端：恢复小组（仅恢复操作，解散请走 DELETE /admin/{id}）
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TEACHER')")
    @PutMapping("/admin/{id}/status")
    public Result<?> adminToggleStatus(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        Integer newStatus = params.getOrDefault("status", 1);
        if (newStatus == 0) {
            // 解散操作走统一接口（带通知）
            return Result.error("请使用解散按钮操作（DELETE /admin/{id}）");
        }
        // 恢复小组
        StudyGroup group = new StudyGroup();
        group.setId(id);
        group.setStatus(1);
        group.setUpdatedAt(new Date());
        groupMapper.updateById(group);
        // 恢复时将成员也恢复
        StudyGroupMember memberUpdate = new StudyGroupMember();
        memberUpdate.setStatus(1);
        groupMemberMapper.update(memberUpdate, new LambdaQueryWrapper<StudyGroupMember>()
                .eq(StudyGroupMember::getGroupId, id));
        return Result.success("已恢复");
    }

    /**
     * 管理端：强制解散小组（系统管理员，无视成员数限制，自动通知所有成员）
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/admin/{id}")
    public Result<?> adminDismiss(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String nickname = (String) request.getAttribute("nickname");
        // 系统管理员强制解散：isSuperAdmin=true，触发通知机制
        groupService.dismissGroupWithCheck(id, userId, true, nickname);
        return Result.success("已强制解散小组，所有成员已收到通知");
    }

    /**
     * 管理端：成员列表（含已退出）
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TEACHER')")
    @GetMapping("/admin/{id}/members")
    public Result<List<StudyGroupMember>> adminMembers(@PathVariable Long id,
                                                         @RequestParam(required = false) Integer status) {
        // 使用 JOIN 查询带出用户信息
        List<StudyGroupMember> all = groupMemberMapper.selectAllMembersWithUser(id);
        if (status != null) {
            all = all.stream().filter(m -> status.equals(m.getStatus())).collect(java.util.stream.Collectors.toList());
        }
        return Result.success(all);
    }

    /**
     * 管理端：踢出成员
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TEACHER')")
    @DeleteMapping("/admin/member/{memberId}")
    public Result<?> adminKickMember(@PathVariable Long memberId) {
        StudyGroupMember member = groupMemberMapper.selectById(memberId);
        if (member == null) return Result.error("成员不存在");
        if (member.getRole() == 2) return Result.error("不能踢出组长，请先转让组长");
        member.setStatus(0);
        groupMemberMapper.updateById(member);
        // 更新小组人数
        StudyGroup group = groupMapper.selectById(member.getGroupId());
        if (group != null) {
            group.setCurrentMembers(Math.max(group.getCurrentMembers() - 1, 1));
            groupMapper.updateById(group);
        }
        return Result.success("已移出小组");
    }

    /**
     * 管理端：修改成员角色
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TEACHER')")
    @PutMapping("/admin/member/{memberId}/role")
    public Result<?> adminChangeRole(@PathVariable Long memberId, @RequestBody Map<String, Integer> params) {
        Integer role = params.get("role");
        StudyGroupMember member = groupMemberMapper.selectById(memberId);
        if (member == null) return Result.error("成员不存在");
        member.setRole(role);
        member.setUpdatedAt(new Date());
        groupMemberMapper.updateById(member);
        return Result.success("角色已修改");
    }

    /**
     * 管理端：任务列表（跨小组）
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TEACHER')")
    @GetMapping("/admin/tasks")
    public Result<PageResult<Map<String, Object>>> adminTasks(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<StudyGroupTask> wrapper = new LambdaQueryWrapper<>();
        if (groupId != null) wrapper.eq(StudyGroupTask::getGroupId, groupId);
        if (status != null) wrapper.eq(StudyGroupTask::getStatus, status);
        if (keyword != null && !keyword.isBlank()) wrapper.like(StudyGroupTask::getTaskTitle, keyword);
        wrapper.orderByDesc(StudyGroupTask::getCreatedAt);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<StudyGroupTask> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        var result = groupTaskMapper.selectPage(p, wrapper);
        // 附加小组名
        List<Map<String, Object>> records = new ArrayList<>();
        for (StudyGroupTask t : result.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("groupId", t.getGroupId());
            m.put("taskTitle", t.getTaskTitle());
            m.put("taskContent", t.getTaskContent());
            m.put("deadline", t.getDeadline());
            m.put("completedCount", t.getCompletedCount());
            m.put("status", t.getStatus());
            m.put("createdAt", t.getCreatedAt());
            StudyGroup g = groupMapper.selectById(t.getGroupId());
            m.put("groupName", g != null ? g.getGroupName() : "未知");
            records.add(m);
        }
        return Result.success(PageResult.of(records, result.getTotal(), result.getSize(), result.getCurrent()));
    }

    /**
     * 管理端：删除任务
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TEACHER')")
    @DeleteMapping("/admin/task/{id}")
    public Result<?> adminDeleteTask(@PathVariable Long id) {
        groupTaskMapper.deleteById(id);
        return Result.success("已删除");
    }

    /**
     * 管理端：资源列表（跨小组）
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TEACHER')")
    @GetMapping("/admin/resources")
    public Result<PageResult<Map<String, Object>>> adminResources(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<StudyGroupResource> wrapper = new LambdaQueryWrapper<>();
        if (groupId != null) wrapper.eq(StudyGroupResource::getGroupId, groupId);
        if (keyword != null && !keyword.isBlank()) wrapper.like(StudyGroupResource::getFileName, keyword);
        wrapper.orderByDesc(StudyGroupResource::getCreatedAt);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<StudyGroupResource> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        var result = groupResourceMapper.selectPage(p, wrapper);
        List<Map<String, Object>> records = new ArrayList<>();
        for (StudyGroupResource r : result.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("groupId", r.getGroupId());
            m.put("fileName", r.getFileName());
            m.put("fileUrl", r.getFileUrl());
            m.put("fileSize", r.getFileSize());
            m.put("downloadCount", r.getDownloadCount());
            m.put("createdAt", r.getCreatedAt());
            StudyGroup g = groupMapper.selectById(r.getGroupId());
            m.put("groupName", g != null ? g.getGroupName() : "未知");
            records.add(m);
        }
        return Result.success(PageResult.of(records, result.getTotal(), result.getSize(), result.getCurrent()));
    }

    /**
     * 管理端：删除资源
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TEACHER')")
    @DeleteMapping("/admin/resource/{id}")
    public Result<?> adminDeleteResource(@PathVariable Long id) {
        groupResourceMapper.deleteById(id);
        return Result.success("已删除");
    }
}
