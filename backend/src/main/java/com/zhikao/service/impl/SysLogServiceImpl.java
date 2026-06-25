package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.SysLoginLog;
import com.zhikao.entity.SysOperLog;
import com.zhikao.mapper.SysLoginLogMapper;
import com.zhikao.mapper.SysOperLogMapper;
import com.zhikao.service.SysLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 系统日志Service实现
 */
@Service
public class SysLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements SysLogService {

    @Autowired
    private SysLoginLogMapper sysLoginLogMapper;

    @Override
    public void recordOper(SysOperLog operLog) {
        if (operLog == null) return;
        try {
            // 截断超长字段，避免写入失败
            if (operLog.getParams() != null && operLog.getParams().length() > 2000) {
                operLog.setParams(operLog.getParams().substring(0, 2000));
            }
            if (operLog.getErrorMsg() != null && operLog.getErrorMsg().length() > 1000) {
                operLog.setErrorMsg(operLog.getErrorMsg().substring(0, 1000));
            }
            save(operLog);
        } catch (Exception ignored) {
            // 日志写入失败不应影响主业务
        }
    }

    @Override
    public void recordLogin(SysLoginLog loginLog) {
        if (loginLog == null) return;
        try {
            if (loginLog.getMsg() != null && loginLog.getMsg().length() > 200) {
                loginLog.setMsg(loginLog.getMsg().substring(0, 200));
            }
            sysLoginLogMapper.insert(loginLog);
        } catch (Exception ignored) {
        }
    }

    @Override
    public IPage<SysOperLog> pageOperLogs(long current, long size, String username, Integer status) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isEmpty()) {
            wrapper.like(SysOperLog::getUsername, username);
        }
        if (status != null) {
            wrapper.eq(SysOperLog::getStatus, status);
        }
        wrapper.orderByDesc(SysOperLog::getCreatedAt);
        return page(new Page<>(current, size), wrapper);
    }

    @Override
    public IPage<SysLoginLog> pageLoginLogs(long current, long size, String username, Integer status) {
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isEmpty()) {
            wrapper.like(SysLoginLog::getUsername, username);
        }
        if (status != null) {
            wrapper.eq(SysLoginLog::getStatus, status);
        }
        wrapper.orderByDesc(SysLoginLog::getCreatedAt);
        return sysLoginLogMapper.selectPage(new Page<>(current, size), wrapper);
    }
}
