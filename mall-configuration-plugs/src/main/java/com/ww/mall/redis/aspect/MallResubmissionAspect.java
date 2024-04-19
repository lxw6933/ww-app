package com.ww.mall.redis.aspect;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ArrayUtil;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.redis.annotation.MallResubmission;
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
import org.springframework.util.StringUtils;

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

    @Around("@annotation(com.ww.mall.redis.annotation.MallResubmission)")
    public Object mallResubmissionAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        MallResubmission mallResubmission = method.getAnnotation(MallResubmission.class);
        String parameterKey = generateParameterKey(method.getName(), args, mallResubmission);
        final Boolean success = redisTemplate.execute(
                (RedisCallback<Boolean>) connection -> connection.set(parameterKey.getBytes(), new byte[0], Expiration.from(mallResubmission.expire(), mallResubmission.timeUnit())
                        , RedisStringCommands.SetOption.SET_IF_ABSENT));
        if (!Boolean.TRUE.equals(success)) {
            throw new ApiException("操作过快，请稍候再试");
        }
        return joinPoint.proceed();
    }

    private String generateParameterKey(String methodName, Object[] args, MallResubmission mallResubmission) {
        String delimiter = mallResubmission.delimiter();
        String prefix = mallResubmission.prefix();
        StringBuilder keyBuilder = new StringBuilder();
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(prefix)) {
            keyBuilder.append(prefix).append(delimiter);
        }
        keyBuilder.append(methodName);
        if (ArrayUtil.isNotEmpty(args)) {
            String key = StringUtils.arrayToDelimitedString(args, delimiter);
            String encode = Base64.encode(key.getBytes());
            keyBuilder.append(delimiter).append(encode);
        }
        return keyBuilder.toString();
    }

}
