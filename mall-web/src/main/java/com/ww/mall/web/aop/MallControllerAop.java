package com.ww.mall.web.aop;

import cn.hutool.json.JSONUtil;
import com.ww.mall.common.utils.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author ww
 * @create 2024-09-04- 14:23
 * @description:
 */
@Slf4j
@Aspect
@Component
public class MallControllerAop {

    /**
     * 控制器切点
     */
    @Pointcut(value = "execution(* com.ww.mall.*.controller..*.*(..))")
    public void controllerPointcut() {}

    /**
     * 前置织入
     *
     * @param joinPoint 连接点
     */
    @Before(value = "controllerPointcut()")
    public void before(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String ip;
        if (attributes == null) {
            ip = "unknown";
        } else {
            HttpServletRequest request = attributes.getRequest();
            ip = IpUtil.getRealIp(request);
        }
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        String params = JSONUtil.toJsonStr(args);
        log.info("IP:【{}】请求进入 [{}#{}] 请求参数为: {}", ip, className, methodName, params);
    }

    /**
     * 正常返回织入
     *
     * @param joinPoint 连接点
     * @param result    返回值
     */
    @AfterReturning(value = "controllerPointcut()", returning = "result")
    public void afterReturn(JoinPoint joinPoint, Object result) {
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();
        String resultJson = JSONUtil.toJsonStr(result);
        log.info("请求返回 [{}#{}] 响应参数为: {}", className, methodName, resultJson);
    }

    /**
     * 异常织入
     *
     * @param joinPoint 连接点
     * @param ex        异常
     */
    @AfterThrowing(value = "controllerPointcut()", throwing = "ex")
    public void afterThrowing(JoinPoint joinPoint, Throwable ex) {
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();
        log.error("请求 [{}#{}] 发生异常: {}", className, methodName, ex.getMessage());
    }

}
