package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhikao.common.PageResult;
import com.zhikao.entity.*;
import com.zhikao.mapper.*;
import com.zhikao.service.GroupService;
import com.zhikao.service.SysMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 学习小组Service实现类
 */
@Service
public class GroupServiceImpl implements GroupService {

    @Autowired
    private StudyGroupMapper groupMapper;
    @Autowired
    private StudyGroupMemberMapper groupMemberMapper;
    @Autowired
    private StudyGroupTaskMapper groupTaskMapper;
    @Autowired
    private StudyGroupResourceMapper groupResourceMapper;
    @Autowired
    private SysMessageService sysMessageService;

    @Override
    public PageResult<StudyGroup> listGroups(Integer page, Integer size, String keyword, Integer joinType) {
        LambdaQueryWrapper<StudyGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudyGroup::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(StudyGroup::getGroupName, keyword);
        }
        if (joinType != null) {
            wrapper.eq(StudyGroup::getJoinType, joinType);
        }
        wrapper.orderByDesc(StudyGroup::getActiveScore)
               .orderByDesc(StudyGroup::getCreatedAt);

        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;

        Page<StudyGroup> mpPage = new Page<>(page, size);
        Page<StudyGroup> result = groupMapper.selectPage(mpPage, wrapper);

        return PageResult.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    public StudyGroup getGroup(Long id) {
        return groupMapper.selectById(id);
    }

    @Override
    @Transactional
    public StudyGroup createGroup(StudyGroup group, Long userId) {
        // 检查用户是否已创建了小组（可选限制）
        group.setCreatorId(userId);
        group.setCurrentMembers(1);
        group.setLevel(1);
        group.setActiveScore(0);
        group.setStatus(1);
        group.setCreatedAt(new Date());
        group.setUpdatedAt(new Date());
        if (group.getMaxMembers() == null) group.setMaxMembers(200);
        if (group.getJoinType() == null) group.setJoinType(1);
        groupMapper.insert(group);

        // 创建人自动成为组长
        StudyGroupMember member = new StudyGroupMember();
        member.setGroupId(group.getId());
        member.setUserId(userId);
        member.setRole(2); // 组长
        member.setContribution(0);
        member.setJoinTime(new Date());
        member.setStatus(1);
        member.setCreatedAt(new Date());
        member.setUpdatedAt(new Date());
        groupMemberMapper.insert(member);

        return group;
    }

    @Override
    @Transactional
    public void joinGroup(Long groupId, Long userId) {
        // 检查是否已是成员
        LambdaQueryWrapper<StudyGroupMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(StudyGroupMember::getGroupId, groupId)
                .eq(StudyGroupMember::getUserId, userId)
                .eq(StudyGroupMember::getStatus, 1);
        if (groupMemberMapper.selectCount(memberWrapper) > 0) {
            throw new RuntimeException("您已是该小组成员");
        }

        // 检查小组是否存在
        StudyGroup group = groupMapper.selectById(groupId);
        if (group == null || group.getStatus() == 0) {
            throw new RuntimeException("小组不存在或已解散");
        }
        if (group.getCurrentMembers() >= group.getMaxMembers()) {
            throw new RuntimeException("小组已满员");
        }

        // 加入小组
        StudyGroupMember member = new StudyGroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(0); // 普通成员
        member.setContribution(0);
        member.setJoinTime(new Date());
        member.setStatus(1);
        member.setCreatedAt(new Date());
        member.setUpdatedAt(new Date());
        groupMemberMapper.insert(member);

        // 更新小组人数
        group.setCurrentMembers(group.getCurrentMembers() + 1);
        groupMapper.updateById(group);
    }

    @Override
    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        // 查找成员记录
        LambdaQueryWrapper<StudyGroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudyGroupMember::getGroupId, groupId)
                .eq(StudyGroupMember::getUserId, userId)
                .eq(StudyGroupMember::getStatus, 1);
        StudyGroupMember member = groupMemberMapper.selectOne(wrapper);
        if (member == null) {
            throw new RuntimeException("您不在该小组中");
        }

        // 组长不能直接退出，需先转让或解散
        if (member.getRole() == 2) {
            throw new RuntimeException("组长不能直接退出，请先转让组长或解散小组");
        }

        member.setStatus(0);
        groupMemberMapper.updateById(member);

        // 更新小组人数
        StudyGroup group = groupMapper.selectById(groupId);
        group.setCurrentMembers(Math.max(group.getCurrentMembers() - 1, 1));
        groupMapper.updateById(group);
    }

    @Override
    @Transactional
    public void dismissGroup(Long groupId, Long userId) {
        // 旧接口保留兼容，委托给新方法（非超管）
        dismissGroupWithCheck(groupId, userId, false, null);
    }

    /**
     * 解散小组（带严格权限校验 + 通知机制）
     *
     * 权限规则：
     * 1. 小组管理员/组长：只能解散「除了自己没有其他成员」的小组
     * 2. 系统管理员：可强制解散任意小组（无视成员数限制）
     *
     * 通知规则：
     * - 系统管理员强制解散时，自动向小组所有成员发送「小组已解散」通知
     * - 小组管理员自行解散（无其他成员）不发送通知（因为只有自己）
     */
    @Override
    @Transactional
    public void dismissGroupWithCheck(Long groupId, Long userId, boolean isSuperAdmin, String operatorName) {
        // 1. 校验小组是否存在
        StudyGroup group = groupMapper.selectById(groupId);
        if (group == null || group.getStatus() == 0) {
            throw new RuntimeException("小组不存在或已解散");
        }

        // 2. 查询当前小组所有活跃成员
        LambdaQueryWrapper<StudyGroupMember> activeMemberWrapper = new LambdaQueryWrapper<>();
        activeMemberWrapper.eq(StudyGroupMember::getGroupId, groupId)
                .eq(StudyGroupMember::getStatus, 1);
        List<StudyGroupMember> activeMembers = groupMemberMapper.selectList(activeMemberWrapper);

        if (isSuperAdmin) {
            // ===== 系统管理员强制解散 =====
            // 无视成员数限制，直接解散

            // 2a. 发送解散通知给所有成员（含小组管理员和普通成员）
            String title = "学习小组已解散：" + group.getGroupName();
            String content = String.format(
                    "您所在的学习小组「%s」已被系统管理员%s解散，如有疑问请联系管理员。",
                    group.getGroupName(),
                    operatorName != null ? "（" + operatorName + "）" : ""
            );
            for (StudyGroupMember m : activeMembers) {
                // 不给操作者自己发通知（如果系统管理员碰巧也是成员）
                if (!m.getUserId().equals(userId)) {
                    sysMessageService.sendMessage(
                            m.getUserId(),
                            1,  // messageType=1 系统通知
                            title,
                            content,
                            groupId,
                            "/group"
                    );
                }
            }

        } else {
            // ===== 小组管理员/组长解散 =====
            // 验证操作者是否为小组成员且角色为管理员或组长
            StudyGroupMember operatorMember = activeMembers.stream()
                    .filter(m -> m.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);

            if (operatorMember == null) {
                throw new RuntimeException("您不是该小组成员，无法解散");
            }
            if (operatorMember.getRole() != 2 && operatorMember.getRole() != 1) {
                throw new RuntimeException("只有组长或管理员才能解散小组");
            }

            // 严格限制：除了操作者自己，不能有其他成员
            long otherMemberCount = activeMembers.stream()
                    .filter(m -> !m.getUserId().equals(userId))
                    .count();

            if (otherMemberCount > 0) {
                throw new RuntimeException(
                        "请先移除所有成员或转移管理员权限后再解散（当前还有 " + otherMemberCount + " 名其他成员）"
                );
            }
        }

        // 3. 执行解散：更新小组状态
        group.setStatus(0);
        group.setUpdatedAt(new Date());
        groupMapper.updateById(group);

        // 4. 将所有活跃成员状态标记为退出
        StudyGroupMember updateMember = new StudyGroupMember();
        updateMember.setStatus(0);
        updateMember.setUpdatedAt(new Date());
        groupMemberMapper.update(updateMember, activeMemberWrapper);

        // 5. 将进行中的任务标记为已取消
        LambdaQueryWrapper<StudyGroupTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(StudyGroupTask::getGroupId, groupId)
                .eq(StudyGroupTask::getStatus, 1);
        StudyGroupTask taskUpdate = new StudyGroupTask();
        taskUpdate.setStatus(0); // 已取消
        taskUpdate.setUpdatedAt(new Date());
        groupTaskMapper.update(taskUpdate, taskWrapper);
    }

    @Override
    public List<StudyGroupMember> listMembers(Long groupId) {
        // 使用 JOIN 查询带出用户名、昵称、头像
        return groupMemberMapper.selectMembersWithUser(groupId);
    }

    @Override
    @Transactional
    public StudyGroupTask createTask(Long groupId, StudyGroupTask task, Long userId) {
        // 验证是否为小组成员
        LambdaQueryWrapper<StudyGroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudyGroupMember::getGroupId, groupId)
                .eq(StudyGroupMember::getUserId, userId)
                .eq(StudyGroupMember::getStatus, 1);
        if (groupMemberMapper.selectCount(wrapper) == 0) {
            throw new RuntimeException("您不是小组成员，不能发布任务");
        }

        task.setGroupId(groupId);
        task.setCreatorId(userId);
        task.setCompletedCount(0);
        task.setStatus(1);
        task.setCreatedAt(new Date());
        task.setUpdatedAt(new Date());
        groupTaskMapper.insert(task);
        return task;
    }

    @Override
    public List<StudyGroupTask> listTasks(Long groupId, Integer status) {
        LambdaQueryWrapper<StudyGroupTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudyGroupTask::getGroupId, groupId);
        if (status != null) {
            wrapper.eq(StudyGroupTask::getStatus, status);
        }
        wrapper.orderByDesc(StudyGroupTask::getCreatedAt);
        return groupTaskMapper.selectList(wrapper);
    }

    @Override
    public List<StudyGroupResource> listResources(Long groupId) {
        LambdaQueryWrapper<StudyGroupResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudyGroupResource::getGroupId, groupId)
                .orderByDesc(StudyGroupResource::getCreatedAt);
        return groupResourceMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public StudyGroupResource uploadResource(Long groupId, StudyGroupResource resource, Long userId) {
        // 验证是否为小组成员
        LambdaQueryWrapper<StudyGroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudyGroupMember::getGroupId, groupId)
                .eq(StudyGroupMember::getUserId, userId)
                .eq(StudyGroupMember::getStatus, 1);
        if (groupMemberMapper.selectCount(wrapper) == 0) {
            throw new RuntimeException("您不是小组成员，不能上传资源");
        }

        resource.setGroupId(groupId);
        resource.setUploaderId(userId);
        resource.setDownloadCount(0);
        resource.setCreatedAt(new Date());
        groupResourceMapper.insert(resource);
        return resource;
    }
}
