package com.ww.app.open.aspect;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.alibaba.fastjson.JSON;
import com.ww.app.open.common.OpenApiContext;
import com.ww.app.open.entity.OpenApiCallLog;
import com.ww.app.open.service.OpenApiCallLogService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 开放平台API日志切面
 * 
 * @author ww
 * @create 2024-05-27
 * @description: 记录API调用日志和统计信息
 */
@Slf4j
@Aspect
@Component
public class OpenApiLogAspect {

    @Resource
    private OpenApiCallLogService openApiCallLogService;

    @Around("execution(* com.ww.app.open.applications..*.*(..)) && @annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        OpenApiContext context = OpenApiContext.get();
        if (context == null) {
            return joinPoint.proceed();
        }

        OpenApiCallLog callLog = new OpenApiCallLog();
        callLog.setTransId(context.getTransId());
        callLog.setAppCode(context.getAppCode());
        callLog.setSysCode(context.getSysCode());
        callLog.setApiCode(context.getApiCode());
        callLog.setRequestIp(context.getRequestIp());
        callLog.setRequestTime(context.getRequestStartTime());

        long startTime = System.currentTimeMillis();
        Object result;

        try {
            // 记录请求参数（脱敏处理）- 从context中获取
            if (context.getOpenRequest() != null) {
                callLog.setRequestParams(JSON.toJSONString(context.getOpenRequest()));
            }

            // 执行方法
            result = joinPoint.proceed();

            // 记录响应结果（脱敏处理）
            if (result != null) {
                callLog.setResponseResult(JSON.toJSONString(result));
            }

            callLog.setSuccess(1);
            callLog.setResponseStatus(200);

        } catch (Exception e) {
            callLog.setSuccess(0);
            callLog.setErrorMessage(ExceptionUtil.getRootCauseMessage(e));
            callLog.setResponseStatus(500);
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            callLog.setResponseTime(endTime);
            callLog.setDuration(endTime - startTime);

            // 异步保存日志 + 统计
            openApiCallLogService.saveCallLogAsync(callLog);
        }

        return result;
    }

}

