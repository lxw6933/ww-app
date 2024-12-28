package com.ww.mall.web.aop;

import cn.hutool.json.JSONUtil;
import com.ww.mall.common.utils.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ww
 * @create 2024-09-04- 14:23
 * @description:
 */
@Slf4j
@Aspect
@Component
public class ControllerAop {

    /**
     * 控制器切点
     */
    @Pointcut(value = "execution(* com.ww.mall.*.controller..*.*(..))")
    public void controllerPointcut() {
    }

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
        // 过滤掉不需要转json的参数
        List<Object> targetArgs = Arrays.stream(args)
                .filter(e -> !(e instanceof MultipartFile || e instanceof HttpServletRequest || e instanceof HttpServletResponse || e instanceof BindingResult))
                .collect(Collectors.toList());
        String params = JSONUtil.toJsonStr(targetArgs);
        log.info("IP:[{}]请求 [{}#{}] 请求参数: {}", ip, className, methodName, params);
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
        log.info("请求 [{}#{}] 响应参数: {}", className, methodName, resultJson);
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
