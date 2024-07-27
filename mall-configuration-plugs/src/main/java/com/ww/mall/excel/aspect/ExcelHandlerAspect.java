package com.ww.mall.excel.aspect;

import com.ww.mall.redis.aspect.MallAbstractAspect;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * @author ww
 * @create 2024-06-01 10:41
 * @description:
 */
@Slf4j
@Aspect
@Component
public class ExcelHandlerAspect extends MallAbstractAspect {

    @Around("@annotation(com.ww.mall.annotation.plugs.excel.ExcelExportTimer)")
    public Object exportExcel(ProceedingJoinPoint joinPoint) {
        long startTime = System.nanoTime();
        log.info("开始导出：{}", joinPoint.getSignature().getName());
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            Duration time = Duration.ofNanos(System.nanoTime() - startTime);
            log.info("导出结束，消耗了：{}s", time.getSeconds());
        }
    }

    @Around("@annotation(com.ww.mall.annotation.plugs.excel.ExcelImportTimer)")
    public Object importExcel(ProceedingJoinPoint joinPoint) {
        long startTime = System.nanoTime();
        log.info("开始导入：{}", joinPoint.getSignature().getName());
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            Duration time = Duration.ofNanos(System.nanoTime() - startTime);
            log.info("导入结束，消耗了：{}s", time.getSeconds());
        }
    }

}
