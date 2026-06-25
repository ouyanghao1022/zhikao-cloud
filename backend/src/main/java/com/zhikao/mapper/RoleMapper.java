package com.zhikao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhikao.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色Mapper
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据用户ID查询角色列表
     */
    @Select("""
            SELECT r.* FROM sys_role r
            LEFT JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND r.status = 1
            """)
    List<Role> selectRolesByUserId(Long userId);

    /**
     * 根据角色编码查询
     */
    @Select("SELECT * FROM sys_role WHERE role_code = #{roleCode} AND status = 1")
    Role selectByRoleCode(String roleCode);
}
