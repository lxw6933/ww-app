package com.ww.mall.redis.aspect;

import com.ww.mall.redis.annotation.MallRedisPublishMsg;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @author ww
 * @create 2023-11-30- 13:54
 * @description: redis发布订阅
 */
@Slf4j
@Aspect
@Component
public class MallRedisPublishAspect extends MallAbstractAspect{

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Around("@annotation(com.ww.mall.redis.annotation.MallRedisPublishMsg)")
    public Object mallDistributedLockAdvise(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        // 获取方法参数名
        String[] parameterNames = new LocalVariableTableParameterNameDiscoverer().getParameterNames(method);
        // 获取方法参数
        Object[] parameterValues = joinPoint.getArgs();
        // 构建SpEL上下文，并设置变量值
        MyStandardEvaluationContext elContext = new MyStandardEvaluationContext(parameterNames, parameterValues);
        MallRedisPublishMsg mallRedisPublishMsg = method.getAnnotation(MallRedisPublishMsg.class);
        Object channelName = parser.parseExpression(mallRedisPublishMsg.value()).getValue(elContext);
        Object message = parser.parseExpression(mallRedisPublishMsg.message()).getValue(elContext);
        Object proceed = joinPoint.proceed();
        if (Objects.nonNull(channelName) && Objects.nonNull(message)) {
            log.info("发布redis订阅渠道【{}】消息【{}】", channelName, message);
            redisTemplate.convertAndSend((String) channelName, message);
        }
        return proceed;
    }

}
