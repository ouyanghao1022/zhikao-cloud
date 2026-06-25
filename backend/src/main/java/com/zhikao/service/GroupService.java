package com.zhikao.service;

import com.zhikao.common.PageResult;
import com.zhikao.entity.*;

import java.util.List;

/**
 * 学习小组Service接口
 */
public interface GroupService {

    /** 小组列表（分页） */
    PageResult<StudyGroup> listGroups(Integer page, Integer size, String keyword, Integer joinType);

    /** 小组详情 */
    StudyGroup getGroup(Long id);

    /** 创建小组 */
    StudyGroup createGroup(StudyGroup group, Long userId);

    /** 加入小组 */
    void joinGroup(Long groupId, Long userId);

    /** 退出小组 */
    void leaveGroup(Long groupId, Long userId);

    /** 解散小组（组长/管理员，有成员限制） */
    void dismissGroup(Long groupId, Long userId);

    /**
     * 解散小组（带权限校验）
     * @param groupId 小组ID
     * @param userId  操作者ID
     * @param isSuperAdmin 是否为系统管理员（强制解散，不受成员数限制）
     * @param operatorName 操作者名称（用于通知）
     */
    void dismissGroupWithCheck(Long groupId, Long userId, boolean isSuperAdmin, String operatorName);

    /** 成员列表 */
    List<StudyGroupMember> listMembers(Long groupId);

    /** 发布任务 */
    StudyGroupTask createTask(Long groupId, StudyGroupTask task, Long userId);

    /** 任务列表 */
    List<StudyGroupTask> listTasks(Long groupId, Integer status);

    /** 资源列表 */
    List<StudyGroupResource> listResources(Long groupId);

    /** 上传资源 */
    StudyGroupResource uploadResource(Long groupId, StudyGroupResource resource, Long userId);
}
