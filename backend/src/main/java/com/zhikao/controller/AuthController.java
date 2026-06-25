package com.zhikao.controller;

import com.zhikao.common.Result;
import com.zhikao.dto.LoginDTO;
import com.zhikao.dto.RegisterDTO;
import com.zhikao.entity.SysLoginLog;
import com.zhikao.service.RoleService;
import com.zhikao.service.SysLogService;
import com.zhikao.service.UserService;
import com.zhikao.utils.JwtUtils;
import com.zhikao.vo.LoginVO;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器 - 登录/注册/Token刷新
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private SysLogService sysLogService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<?> register(@Validated @RequestBody RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            return Result.badRequest("两次输入的密码不一致");
        }

        try {
            userService.register(dto.getUsername(), dto.getPassword(),
                    dto.getEmail(), dto.getPhone(), dto.getNickname());
            return Result.success("注册成功");
        } catch (RuntimeException e) {
            return Result.badRequest(e.getMessage());
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO dto, HttpServletRequest request) {
        var user = userService.selectByAccount(dto.getAccount());
        if (user == null) {
            recordLogin(request, null, dto.getAccount(), false, "用户不存在");
            return Result.error(401, "用户不存在");
        }
        if (user.getStatus() == 0) {
            recordLogin(request, user.getId(), dto.getAccount(), false, "账号已被禁用");
            return Result.error(403, "账号已被禁用");
        }
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            recordLogin(request, user.getId(), dto.getAccount(), false, "密码错误");
            return Result.error(401, "密码错误");
        }

        // 获取用户角色
        var roles = roleService.getRolesByUserId(user.getId());
        String roleCode = (roles != null && !roles.isEmpty()) ? roles.get(0).getRoleCode() : "STUDENT";

        // 生成双Token
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), roleCode);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());

        // 更新登录信息
        String ipAddress = request.getRemoteAddr();
        userService.updateLoginInfo(user.getId(), ipAddress);

        // 构建响应
        LoginVO vo = new LoginVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(roleCode);
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setAccessTokenExpire(jwtUtils.getAccessTokenExpire());
        vo.setRefreshTokenExpire(jwtUtils.getRefreshTokenExpire());

        recordLogin(request, user.getId(), user.getUsername(), true, "登录成功");
        return Result.success("登录成功", vo);
    }

    /**
     * 记录登录日志
     */
    private void recordLogin(HttpServletRequest request, Long userId, String username,
                             boolean success, String msg) {
        try {
            SysLoginLog loginLog = new SysLoginLog();
            loginLog.setUserId(userId);
            loginLog.setUsername(username);
            loginLog.setIp(request.getRemoteAddr());
            String ua = request.getHeader("User-Agent");
            if (ua != null) {
                try {
                    cn.hutool.http.useragent.UserAgent parsed =
                            cn.hutool.http.useragent.UserAgentUtil.parse(ua);
                    if (parsed != null) {
                        if (parsed.getBrowser() != null) loginLog.setBrowser(parsed.getBrowser().getName());
                        if (parsed.getOs() != null) loginLog.setOs(parsed.getOs().getName());
                    }
                } catch (Exception ignored) {}
            }
            loginLog.setStatus(success ? 1 : 0);
            loginLog.setMsg(msg);
            loginLog.setCreatedAt(new Date());
            sysLogService.recordLogin(loginLog);
        } catch (Exception ignored) {}
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public Result<LoginVO> refreshToken(@RequestParam String refreshToken) {
        if (!jwtUtils.isRefreshToken(refreshToken)) {
            return Result.error(401, "无效的RefreshToken");
        }

        try {
            Long userId = jwtUtils.getUserIdFromToken(refreshToken);
            String username = jwtUtils.getUsernameFromToken(refreshToken);

            // 获取用户角色
            var userRoles = roleService.getRolesByUserId(userId);
            String roleCode = (userRoles != null && !userRoles.isEmpty()) ? userRoles.get(0).getRoleCode() : "STUDENT";

            String newAccessToken = jwtUtils.generateAccessToken(userId, username, roleCode);
            String newRefreshToken = jwtUtils.generateRefreshToken(userId, username);

            LoginVO vo = new LoginVO();
            vo.setAccessToken(newAccessToken);
            vo.setRefreshToken(newRefreshToken);
            vo.setAccessTokenExpire(jwtUtils.getAccessTokenExpire());
            vo.setRefreshTokenExpire(jwtUtils.getRefreshTokenExpire());

            return Result.success("Token刷新成功", vo);
        } catch (Exception e) {
            return Result.error(401, "RefreshToken已过期，请重新登录");
        }
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public Result<Map<String, Object>> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized("未登录");
        }

        var user = userService.getById(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("email", user.getEmail());
        data.put("phone", user.getPhone());
        data.put("school", user.getSchool());
        data.put("grade", user.getGrade());
        data.put("signature", user.getSignature());
        data.put("gender", user.getGender());
        data.put("createdAt", user.getCreatedAt());
        data.put("lastLoginTime", user.getLastLoginTime());
        data.put("loginCount", user.getLoginCount());
        data.put("role", request.getAttribute("role"));

        return Result.success(data);
    }

    /**
     * 退出登录（客户端自行清除Token）
     */
    @PostMapping("/logout")
    public Result<?> logout() {
        return Result.success("退出成功");
    }
}
