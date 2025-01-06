package com.ww.app.sensitive.aspect;

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.SpringExpressionUtils;
import com.ww.app.sensitive.annotation.SensitiveWord;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author ww
 * @create 2024-05-24 21:08
 * @description:
 */
@Order
@Slf4j
@Aspect
@Component
public class SensitiveWordAspect {

    @Resource
    private SensitiveWordBs sensitiveWordBs;

    @Around("@annotation(com.ww.app.sensitive.annotation.SensitiveWord)")
    public Object mallSensitiveWordAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        SensitiveWord sensitiveWord = method.getAnnotation(SensitiveWord.class);
        String[] filterContents = sensitiveWord.content();
        String msg = sensitiveWord.msg();
        if (filterContents == null) {
            return joinPoint.proceed();
        }
        // 解析el表达式
        SpringExpressionUtils.parseExpressions(joinPoint, Arrays.asList(filterContents), (expression, expressContext, elValue) -> {
            boolean includeSensitiveWord = sensitiveWordBs.contains(elValue);
            if (includeSensitiveWord) {
                switch (sensitiveWord.handlerType()) {
                    case EXCEPTION:
                        log.error("[异常]内容包含敏感词,content:[{}]", elValue);
                        throw new ApiException(msg);
                    case REPLACE:
                        expression.setValue(expressContext, sensitiveWordBs.replace(StringUtils.deleteWhitespace(elValue)));
                        log.error("[脱敏]内容包含敏感词,content:[{}]", elValue);
                        break;
                    default:
                }
            }
        });
        return joinPoint.proceed();
    }

}
