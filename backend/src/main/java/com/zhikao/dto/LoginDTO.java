package com.zhikao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录DTO
 */
@Data
public class LoginDTO implements Serializable {

    /** 登录账号：用户名/邮箱/手机号 */
    @NotBlank(message = "登录账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码（可选） */
    private String captcha;

    /** 验证码key */
    private String captchaKey;
}
