package com.ww.app.redis.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.enums.GlobalResCodeConstants;
import com.ww.app.common.exception.ApiException;
import com.ww.app.redis.annotation.Resubmission;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @author ww
 * @create 2023-09-05- 10:40
 * @description:
 */
@Aspect
@Component
public class ResubmissionAspect {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String RESUBMISSION_PREFIX = "resubmission";

    @Around("@annotation(com.ww.app.redis.annotation.Resubmission)")
    public Object mallResubmissionAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Resubmission resubmission = method.getAnnotation(Resubmission.class);
        // 通过方法名+参数 ===> 生成key
        String argsStr = StrUtil.join(Constant.SPLIT, joinPoint.getArgs());
        // 避免key过长，使用md5
        String key = StrUtil.join(Constant.SPLIT, RESUBMISSION_PREFIX, SecureUtil.md5(signature + argsStr));
        final Boolean success = redisTemplate.execute(
                (RedisCallback<Boolean>) connection -> connection.set(key.getBytes(), new byte[0], Expiration.from(resubmission.expire(), resubmission.timeUnit())
                        , RedisStringCommands.SetOption.SET_IF_ABSENT));
        if (!Boolean.TRUE.equals(success)) {
            throw new ApiException(GlobalResCodeConstants.REPEATED_REQUESTS);
        }
        return joinPoint.proceed();
    }

}
