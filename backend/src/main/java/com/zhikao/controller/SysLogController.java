package com.zhikao.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhikao.common.PageRequest;
import com.zhikao.common.PageResult;
import com.zhikao.common.Result;
import com.zhikao.entity.SysLoginLog;
import com.zhikao.entity.SysOperLog;
import com.zhikao.service.SysLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 系统日志控制器（管理员）
 */
@RestController
@RequestMapping("/log")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SysLogController {

    @Autowired
    private SysLogService sysLogService;

    /**
     * 操作日志分页查询
     */
    @GetMapping("/oper")
    public Result<PageResult<SysOperLog>> operLogs(PageRequest pageRequest,
                                                    @RequestParam(required = false) String username,
                                                    @RequestParam(required = false) Integer status) {
        IPage<SysOperLog> page = sysLogService.pageOperLogs(
                pageRequest.getCurrent(), pageRequest.getSize(), username, status);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(),
                page.getSize(), page.getCurrent()));
    }

    /**
     * 登录日志分页查询
     */
    @GetMapping("/login")
    public Result<PageResult<SysLoginLog>> loginLogs(PageRequest pageRequest,
                                                     @RequestParam(required = false) String username,
                                                     @RequestParam(required = false) Integer status) {
        IPage<SysLoginLog> page = sysLogService.pageLoginLogs(
                pageRequest.getCurrent(), pageRequest.getSize(), username, status);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(),
                page.getSize(), page.getCurrent()));
    }
}
