package com.zhikao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.User;
import com.zhikao.mapper.UserMapper;
import com.zhikao.service.UserService;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 用户Service实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User register(String username, String password, String email, String phone, String nickname) {
        // 校验用户名是否已存在
        if (baseMapper.selectByUsername(username) != null) {
            throw new RuntimeException("用户名已存在");
        }
        if (StrUtil.isNotBlank(email) && baseMapper.selectByEmail(email) != null) {
            throw new RuntimeException("邮箱已被注册");
        }
        if (StrUtil.isNotBlank(phone) && baseMapper.selectByPhone(phone) != null) {
            throw new RuntimeException("手机号已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setEmail(email);
        user.setPhone(phone);
        user.setNickname(StrUtil.isNotBlank(nickname) ? nickname : username);
        user.setGender(0);
        user.setStatus(1);
        user.setLoginCount(0);
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());

        save(user);
        return user;
    }

    @Override
    public User selectByAccount(String account) {
        if (account == null || account.isBlank()) {
            return null;
        }
        // 尝试按用户名、邮箱、手机号依次查找
        User user = baseMapper.selectByUsername(account);
        if (user == null && account.contains("@")) {
            user = baseMapper.selectByEmail(account);
        }
        if (user == null && account.matches("^1[3-9]\\d{9}$")) {
            user = baseMapper.selectByPhone(account);
        }
        return user;
    }

    @Override
    public void updateLoginInfo(Long userId, String ipAddress) {
        User user = new User();
        user.setId(userId);
        user.setLoginCount(lambdaQuery().eq(User::getId, userId).one().getLoginCount() + 1);
        user.setLastLoginTime(new Date());
        user.setLastLoginIp(ipAddress);
        user.setUpdatedAt(new Date());
        updateById(user);
    }
}
