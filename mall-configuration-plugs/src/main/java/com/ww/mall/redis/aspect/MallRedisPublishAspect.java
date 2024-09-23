package com.ww.mall.redis.aspect;

import com.alibaba.fastjson.JSON;
import com.ww.mall.redis.MallRedisTemplate;
import com.ww.mall.annotation.plugs.redis.MallRedisPublishMsg;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
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
@ConditionalOnBean(MallRedisTemplate.class)
public class MallRedisPublishAspect extends MallAbstractAspect{

    @Resource
    private MallRedisTemplate mallRedisTemplate;

    @Resource
    private ThreadPoolExecutor defaultThreadPoolExecutor;

    @Around("@annotation(com.ww.mall.annotation.plugs.redis.MallRedisPublishMsg)")
    public Object mallRedisPublishAdvise(ProceedingJoinPoint joinPoint) throws Throwable {
        Object proceed = joinPoint.proceed();
        CompletableFuture.runAsync(() -> {
            try {
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                Method method = signature.getMethod();
                // 获取方法参数名
                String[] parameterNames = new LocalVariableTableParameterNameDiscoverer().getParameterNames(method);
                // 获取方法参数
                Object[] parameterValues = joinPoint.getArgs();
                // 构建SpEL上下文，并设置变量值
                MyStandardEvaluationContext elContext = new MyStandardEvaluationContext(parameterNames, parameterValues);
                MallRedisPublishMsg mallRedisPublishMsg = method.getAnnotation(MallRedisPublishMsg.class);
                String channelName = mallRedisPublishMsg.value();
                Object message = "all";
                if (StringUtils.isNotEmpty(mallRedisPublishMsg.message())) {
                    message = parser.parseExpression(mallRedisPublishMsg.message()).getValue(elContext);
                }
                String messageJson;
                if (message instanceof Collection) {
                    messageJson = JSON.toJSONString(message);
                } else {
                    messageJson = JSON.toJSONString(Collections.singleton(message));
                }
                log.info("发布redis订阅渠道[{}]消息[{}]", channelName, messageJson);
                mallRedisTemplate.publishMessage(channelName, messageJson);
            } catch (Exception e) {
                log.error("发布redis订阅渠道异常：{}", e.getMessage());
            }
        }, defaultThreadPoolExecutor);
        return proceed;
    }

}
