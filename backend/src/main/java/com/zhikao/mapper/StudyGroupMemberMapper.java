package com.zhikao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhikao.entity.StudyGroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学习小组成员Mapper
 */
@Mapper
public interface StudyGroupMemberMapper extends BaseMapper<StudyGroupMember> {

    /**
     * 查询小组成员（JOIN sys_user 带出用户名、昵称、头像），仅活跃成员
     */
    @Select("SELECT m.*, u.username, u.nickname, u.avatar " +
            "FROM study_group_member m " +
            "LEFT JOIN sys_user u ON m.user_id = u.id " +
            "WHERE m.group_id = #{groupId} AND m.status = 1 " +
            "ORDER BY m.role ASC, m.contribution DESC, m.join_time ASC")
    List<StudyGroupMember> selectMembersWithUser(@Param("groupId") Long groupId);

    /**
     * 查询小组成员（含已退出），管理端使用
     */
    @Select("SELECT m.*, u.username, u.nickname, u.avatar " +
            "FROM study_group_member m " +
            "LEFT JOIN sys_user u ON m.user_id = u.id " +
            "WHERE m.group_id = #{groupId} " +
            "ORDER BY m.role ASC, m.contribution DESC, m.join_time ASC")
    List<StudyGroupMember> selectAllMembersWithUser(@Param("groupId") Long groupId);
}
