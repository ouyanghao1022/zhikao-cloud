package com.zhikao.aspect;

import com.alibaba.fastjson.JSON;
import com.zhikao.entity.SysOperLog;
import com.zhikao.service.SysLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Date;

/**
 * 审计日志切面 — 记录 Controller 操作到 sys_oper_log
 */
@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    @Autowired
    @Lazy
    private SysLogService sysLogService;

    @Pointcut("execution(* com.zhikao.controller..*(..)) " +
            "&& !execution(* com.zhikao.controller.AuthController.*(..)) " +
            "&& !execution(* com.zhikao.controller.SysLogController.*(..)) " +
            "&& !execution(* com.zhikao.controller.FileUploadController.*(..))")
    public void controllerPointcut() {}

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        SysOperLog operLog = new SysOperLog();
        operLog.setCreatedAt(new Date());

        // 请求与用户信息
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                operLog.setIp(request.getRemoteAddr());
                operLog.setMethod(request.getMethod() + " " + request.getRequestURI());
                Object userId = request.getAttribute("userId");
                if (userId instanceof Long) {
                    operLog.setUserId((Long) userId);
                }
                Object username = request.getAttribute("username");
                if (username instanceof String) {
                    operLog.setUsername((String) username);
                } else {
                    try {
                        var auth = org.springframework.security.core.context.SecurityContextHolder
                                .getContext().getAuthentication();
                        if (auth != null && auth.getName() != null) {
                            operLog.setUsername(auth.getName());
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}

        operLog.setOperation(joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName());

        // 参数序列化（过滤不可序列化的 Servlet 对象）
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                Object[] safe = Arrays.stream(args)
                        .filter(a -> !(a instanceof HttpServletRequest) && !(a instanceof HttpServletResponse))
                        .toArray();
                String params = JSON.toJSONString(safe);
                operLog.setParams(params);
            }
        } catch (Exception e) {
            operLog.setParams("[serialize-failed:" + e.getClass().getSimpleName() + "]");
        }

        Object result;
        try {
            result = joinPoint.proceed();
            operLog.setStatus(1);
        } catch (Throwable e) {
            operLog.setStatus(0);
            operLog.setErrorMsg(e.getMessage());
            operLog.setDuration((int) (System.currentTimeMillis() - start));
            sysLogService.recordOper(operLog);
            throw e;
        }
        operLog.setDuration((int) (System.currentTimeMillis() - start));
        sysLogService.recordOper(operLog);
        return result;
    }
}
