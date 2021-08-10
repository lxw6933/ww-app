package com.ww.mall.aspect;

import com.google.gson.Gson;
import com.ww.mall.annotation.SysLog;
import com.ww.mall.config.security.entity.MyUserDetails;
import com.ww.mall.mvc.entity.SysLogEntity;
import com.ww.mall.mvc.service.SysLogService;
import com.ww.mall.utils.HttpContextUtils;
import com.ww.mall.utils.IpUtils;
import com.ww.mall.utils.LoginUserUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Date;

/**
 * @description: 系统日志aop
 * @author: ww
 * @create: 2021-05-12 19:39
 */
@Slf4j
@Aspect
@Component
public class SysLogAspect {

    @Resource
    private SysLogService sysLogService;

    @Pointcut("@annotation(com.ww.mall.annotation.SysLog)")
    public void logPointCut() {
        // Do nothing because of pointcut expression.
    }

    @Around("logPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long beginTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        SysLog sysLog = method.getAnnotation(SysLog.class);
        SysLogEntity logEntity = new SysLogEntity();
        // 执行方法
        Object result = point.proceed();
        String className = point.getTarget().getClass().getName();
        String methodName = signature.getName();
        logEntity.setMethod(className + "." + methodName + "()");
        // 请求的参数
        Object[] args = point.getArgs();
        try {
            String params = new Gson().toJson(args[0]);
            logEntity.setParams(params);
        } catch (Exception e) {
            log.warn("系统日志-参数转换异常", e);
        }
        // 获取request
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        // 设置IP地址
        String ip = IpUtils.getIp(request);
        logEntity.setIp(ip);
        // 执行时长(毫秒)
        long time = System.currentTimeMillis() - beginTime;
        // 注解上的描述
        logEntity.setOperation(sysLog.value());
        logEntity.setType(sysLog.type());
        // 保存日志
        logEntity.setTime(time);
        logEntity.setCreateDate(new Date());
        // 获取当前操作者信息
        MyUserDetails currentUser = LoginUserUtils.getCurrentUser();
        if (currentUser != null) {
            logEntity.setUserId(currentUser.getId());
            logEntity.setUsername(currentUser.getUsername());
            logEntity.setCenterId(currentUser.getCenterId());
        }
        // 保存系统日志
        try {
            sysLogService.save(logEntity);
        } catch (Exception e) {
            log.warn("系统日志写入发生异常", e);
        }
        return result;
    }

}
