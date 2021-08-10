package com.ww.mall.aspect;

import com.ww.mall.annotation.Limit;
import com.ww.mall.config.redis.RedisManager;
import com.ww.mall.enums.LimitType;
import com.ww.mall.utils.HttpContextUtils;
import com.ww.mall.utils.IpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * @description: 限流
 * @author: ww
 * @create: 2021/5/21 下午7:38
 **/
@Slf4j
//@Aspect
//@Component
public class LimitAspect extends AspectSupport {

    private RedisManager redisManager;

    @Autowired
    public LimitAspect(RedisManager redisManager) {
        this.redisManager = redisManager;
    }

    @Pointcut("@annotation(com.ww.mall.annotation.Limit)")
    public void limit() {
        // Do nothing
    }

    @Around("limit()")
    public Object doAround(ProceedingJoinPoint point) throws Throwable {
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        Method method = resolveMethod(point);
        //获取接口上的Limit注解对象
        Limit limitAnnotation = method.getAnnotation(Limit.class);
        //获取注解上参数数据
        LimitType limitType = limitAnnotation.limitType();
        String name = limitAnnotation.name();
        String key;
        String ip = IpUtils.getIp(request);
        int limitPeriod = limitAnnotation.period();
        int limitCount = limitAnnotation.count();
        switch (limitType) {
            case IP:
                key = ip;
                break;
            case CUSTOMER:
                key = limitAnnotation.key();
                break;
            default:
                key = StringUtils.upperCase(method.getName());
        }
        /**
        ImmutableList<String> keys =
                ImmutableList.of(StringUtils.join(limitAnnotation.prefix() + "_", key));
        if(!redisManager.hasKey(keys.toString())){
            redisManager.set(keys.toString(),new AtomicInteger(0),limitPeriod);
        }
        AtomicInteger num = (AtomicInteger)redisManager.get(keys.toString());

        log.info("IP:{} 第 {} 次访问key为 {}，描述为 [{}] 的接口", ip, num, keys, name);
        if (num != null && num.incrementAndGet() <= limitCount) {
            redisManager.set(keys.toString(),num,limitPeriod);
            return point.proceed();
        } else {
            redisManager.set(keys.toString(),num,limitPeriod);
            log.warn("IP:{} 第 {} 次访问key为 {}，描述为 [{}] 的接口", ip, num, keys, name);
            throw new LimitAccessException("接口访问超出频率限制");
        }
        String luaScript = buildLuaScript();
        String ss = keys.get(0);
        RedisScript<Number> redisScript = new DefaultRedisScript<>(luaScript, Number.class);
        Number count = redisTemplate.execute(redisScript, keys, limitCount, limitPeriod)
         */
        return point.proceed();
    }

    /**
     * 限流脚本
     * 调用的时候不超过阈值，则直接返回并执行计算器自加。
     * @return lua脚本
     */
    public String buildLuaScript() {
        StringBuilder lua = new StringBuilder();
        lua.append("local c");
        lua.append("\nc = redis.call('get',KEYS[1])");
        // 调用不超过最大值，则直接返回
        lua.append("\nif c and tonumber(c) > tonumber(ARGV[1]) then");
        lua.append("\nreturn c;");
        lua.append("\nend");
        // 执行计算器自加
        lua.append("\nc = redis.call('incr',KEYS[1])");
        lua.append("\nif tonumber(c) == 1 then");
        // 从第一次调用开始限流，设置对应键值的过期
        lua.append("\nredis.call('expire',KEYS[1],ARGV[2])");
        lua.append("\nend");
        lua.append("\nreturn c;");
        return lua.toString();
    }

}
