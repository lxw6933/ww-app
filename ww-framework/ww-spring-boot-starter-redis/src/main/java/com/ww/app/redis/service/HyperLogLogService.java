package com.ww.app.redis.service;

import com.ww.app.redis.component.RedissonComponent;
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

    @Resource
    private RedissonComponent redissonComponent;

    public <T> void addElement(String key, T element) {
        redissonComponent.hyperLogLogAdd(key, element);
    }

    public <T> void addElement(String key, List<T> elementList) {
        redissonComponent.hyperLogLogAddAll(key, elementList);
    }

    public <T> long getCount(String key) {
        return redissonComponent.hyperLogLogCount(key);
    }

}
