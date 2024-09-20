package com.ww.mall.redis.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.ww.mall.annotation.plugs.redis.MallResubmission;
import com.ww.mall.common.enums.GlobalResCodeConstants;
import com.ww.mall.common.exception.ApiException;
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
public class MallResubmissionAspect extends MallAbstractAspect {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Around("@annotation(com.ww.mall.annotation.plugs.redis.MallResubmission)")
    public Object mallResubmissionAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        MallResubmission mallResubmission = method.getAnnotation(MallResubmission.class);
        // 通过方法名+参数 ===> 生成key
        String argsStr = StrUtil.join(mallResubmission.delimiter(), joinPoint.getArgs());
        // 避免key过长，使用md5
        String key = SecureUtil.md5(signature + argsStr);
        final Boolean success = redisTemplate.execute(
                (RedisCallback<Boolean>) connection -> connection.set(key.getBytes(), new byte[0], Expiration.from(mallResubmission.expire(), mallResubmission.timeUnit())
                        , RedisStringCommands.SetOption.SET_IF_ABSENT));
        if (!Boolean.TRUE.equals(success)) {
            throw new ApiException(GlobalResCodeConstants.REPEATED_REQUESTS);
        }
        return joinPoint.proceed();
    }

}
