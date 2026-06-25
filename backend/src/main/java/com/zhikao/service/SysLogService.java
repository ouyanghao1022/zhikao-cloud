package com.zhikao.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhikao.entity.SysLoginLog;
import com.zhikao.entity.SysOperLog;

/**
 * 系统日志Service
 */
public interface SysLogService {

    /** 记录操作日志 */
    void recordOper(SysOperLog operLog);

    /** 记录登录日志 */
    void recordLogin(SysLoginLog loginLog);

    /** 操作日志分页查询 */
    IPage<SysOperLog> pageOperLogs(long current, long size, String username, Integer status);

    /** 登录日志分页查询 */
    IPage<SysLoginLog> pageLoginLogs(long current, long size, String username, Integer status);
}
