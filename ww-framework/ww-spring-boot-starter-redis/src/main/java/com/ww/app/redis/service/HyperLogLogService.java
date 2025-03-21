package com.ww.app.redis.service;

import org.redisson.api.RHyperLogLog;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author ww
 * @create 2024-08-27- 09:09
 * @description:
 */
@Component
public class HyperLogLogService {

    private static final String HYPER_LOG_LOG_KEY = "hyperloglog:";

    @Resource
    private RedissonClient redissonClient;

    public <T> void addElement(String key, T element) {
        RHyperLogLog<T> hyperLogLog = redissonClient.getHyperLogLog(HYPER_LOG_LOG_KEY + key);
        hyperLogLog.add(element);
    }

    public <T> void addElement(String key, List<T> elementList) {
        RHyperLogLog<T> hyperLogLog = redissonClient.getHyperLogLog(HYPER_LOG_LOG_KEY + key);
        hyperLogLog.addAll(elementList);
    }

    public <T> long getCount(String key) {
        RHyperLogLog<T> hyperLogLog = redissonClient.getHyperLogLog(HYPER_LOG_LOG_KEY + key);
        return hyperLogLog.count();
    }

}
