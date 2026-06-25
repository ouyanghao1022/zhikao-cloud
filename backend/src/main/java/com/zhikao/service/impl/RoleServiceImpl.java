package com.zhikao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.Role;
import com.zhikao.mapper.RoleMapper;
import com.zhikao.service.RoleService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色Service实现类
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Override
    public List<Role> getRolesByUserId(Long userId) {
        return baseMapper.selectRolesByUserId(userId);
    }

    @Override
    public List<Role> listEnabledRoles() {
        return lambdaQuery()
                .eq(Role::getStatus, 1)
                .orderByAsc(Role::getSort)
                .list();
    }
}
