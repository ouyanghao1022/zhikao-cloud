package com.zhikao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.entity.User;

/**
 * 用户Service接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    User register(String username, String password, String email, String phone, String nickname);

    /**
     * 根据账号查询用户（支持用户名/邮箱/手机号）
     */
    User selectByAccount(String account);

    /**
     * 更新登录信息
     */
    void updateLoginInfo(Long userId, String ipAddress);
}
