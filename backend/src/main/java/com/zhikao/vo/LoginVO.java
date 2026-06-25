package com.zhikao.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 登录响应VO（双Token）
 */
@Data
public class LoginVO implements Serializable {

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 角色 */
    private String role;

    /** 访问令牌（短时效） */
    private String accessToken;

    /** 刷新令牌（长时效） */
    private String refreshToken;

    /** 访问令牌有效期(毫秒) */
    private Long accessTokenExpire;

    /** 刷新令牌有效期(毫秒) */
    private Long refreshTokenExpire;
}
