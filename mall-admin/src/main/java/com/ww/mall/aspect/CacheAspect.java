package com.ww.mall.aspect;

import com.ww.mall.annotation.Cache;
import com.ww.mall.config.redis.RedisManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * @description: 缓存切面
 * @author: ww
 * @create: 2021-05-24 16:29
 */
@Slf4j
@Aspect
@Component
public class CacheAspect {

    @Resource
    private RedisManager redisManager;

    @Pointcut("@annotation(com.ww.mall.annotation.Cache)")
    public void cachePointCut() {
        // do nothing because pointcut
    }

    @Around("cachePointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Cache annotation = method.getAnnotation(Cache.class);
        String key = annotation.key();
        long timeOut = annotation.timeout();
        TimeUnit unit = annotation.unit();
        boolean model = annotation.mode();
        // 方法参数
        Object[] args = point.getArgs();
        key = key  + getKey(args, model);
        Object result;
        try {
            // 先查询缓存
            result = redisManager.get(key);
            if (result != null) {
                return result;
            }
        } catch (Exception ex) {
            log.error("缓存异常：{}", key,  ex);
        }
        result = point.proceed();
        redisManager.set(key, result, timeOut, unit);
        return result;
    }

    /**
     * 构建key
     * @param args 参数
     * @param model 方式
     * @return String
     */
    private String getKey(Object[] args, boolean model) {
        StringBuilder builder = new StringBuilder();
        for (Object obj : args) {
            if (model) {
                builder.append(obj.toString());
                builder.append("_");
            } else {
                builder.append(JsonUtils.toJson(obj));
            }
        }
        String str = builder.substring(0, builder.length() - 1);
        return model ? DigestUtils.md5Hex(str) :  str;
    }

}
