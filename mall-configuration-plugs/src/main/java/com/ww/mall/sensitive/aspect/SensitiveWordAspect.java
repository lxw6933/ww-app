package com.ww.mall.sensitive.aspect;

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.redis.aspect.MallAbstractAspect;
import com.ww.mall.annotation.plugs.sensitive.MallSensitiveWordHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;

/**
 * @author ww
 * @create 2024-05-24 21:08
 * @description:
 */
@Order
@Slf4j
@Aspect
@Component
public class SensitiveWordAspect extends MallAbstractAspect {

    @Resource
    private SensitiveWordBs sensitiveWordBs;

    @Around("@annotation(com.ww.mall.annotation.plugs.sensitive.MallSensitiveWordHandler)")
    public Object mallSensitiveWordAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] parameterValues = joinPoint.getArgs();
        // 获取方法参数名
        String[] parameterNames = new LocalVariableTableParameterNameDiscoverer().getParameterNames(method);
        // 构建SpEL上下文，并设置变量值
        MyStandardEvaluationContext elContext = new MyStandardEvaluationContext(parameterNames, parameterValues);
        MallSensitiveWordHandler mallSensitiveWordHandler = method.getAnnotation(MallSensitiveWordHandler.class);
        String[] filterContents = mallSensitiveWordHandler.content();
        if (filterContents == null) {
            return joinPoint.proceed();
        }
        for (String contentElKey : filterContents) {
            if (StringUtils.isBlank(contentElKey)) {
                continue;
            }
            // 解析el表达式
            String paramContent = parser.parseExpression(contentElKey).getValue(elContext, String.class);
            boolean includeSensitiveWord = sensitiveWordBs.contains(paramContent);
            if (includeSensitiveWord) {
                switch (mallSensitiveWordHandler.handlerType()) {
                    case EXCEPTION:
                        log.error("【异常】内容包含敏感词,key:【{}】content:【{}】", contentElKey, paramContent);
                        throw new ApiException("内容存在非法字符");
                    case REPLACE:
                        parser.parseExpression(contentElKey).setValue(elContext, sensitiveWordBs.replace(StringUtils.deleteWhitespace(paramContent)));
                        log.error("【脱敏】内容包含敏感词,key:【{}】content:【{}】", contentElKey, paramContent);
                        break;
                    default:
                }
            }
        }
        return joinPoint.proceed();
    }

}
