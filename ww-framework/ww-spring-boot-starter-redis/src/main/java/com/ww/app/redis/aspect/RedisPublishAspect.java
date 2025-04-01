package com.ww.app.redis.aspect;

import com.alibaba.fastjson.JSON;
import com.ww.app.common.utils.SpringExpressionUtils;
import com.ww.app.redis.AppRedisTemplate;
import com.ww.app.redis.annotation.RedisPublishMsg;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author ww
 * @create 2023-11-30- 13:54
 * @description: redis发布订阅
 */
@Slf4j
@Aspect
@Component
@ConditionalOnBean(AppRedisTemplate.class)
public class RedisPublishAspect {

    @Resource
    private AppRedisTemplate appRedisTemplate;

    @Resource
    private ThreadPoolExecutor defaultThreadPoolExecutor;

    @Around("@annotation(com.ww.app.redis.annotation.RedisPublishMsg)")
    public Object redisPublishAdvise(ProceedingJoinPoint joinPoint) throws Throwable {
        Object proceed = joinPoint.proceed();
        CompletableFuture.runAsync(() -> {
            try {
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                Method method = signature.getMethod();
                // 构建SpEL上下文，并设置变量值
                RedisPublishMsg redisPublishMsg = method.getAnnotation(RedisPublishMsg.class);
                String channelName = redisPublishMsg.value();
                Object message = "all";
                if (StringUtils.isNotEmpty(redisPublishMsg.message())) {
                    message = SpringExpressionUtils.parseExpression(joinPoint, redisPublishMsg.message());
                }
                String messageJson;
                if (message instanceof Collection) {
                    messageJson = JSON.toJSONString(message);
                } else {
                    messageJson = JSON.toJSONString(Collections.singleton(message));
                }
                log.info("发布redis订阅渠道[{}]消息[{}]", channelName, messageJson);
                appRedisTemplate.publishMessage(channelName, messageJson);
            } catch (Exception e) {
                log.error("发布redis订阅渠道异常：{}", e.getMessage());
            }
        }, defaultThreadPoolExecutor);
        return proceed;
    }

}
