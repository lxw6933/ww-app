package com.ww.mall.seckill.run;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ww.mall.seckill.manager.CacheManager;
import com.ww.mall.seckill.subscriber.RedisCacheMsgListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * @author ww
 * @create 2024-04-11- 09:23
 * @description:
 */
@Slf4j
@Component
public class SystemCacheRunner implements ApplicationRunner {

    @Autowired
    private RedisMessageListenerContainer redisMessageListenerContainer;

    @Autowired
    private RedisCacheMsgListener redisCacheMsgListener;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始加载系统缓存...");
        for (int i = 0; i < 10; i++) {
            CacheManager.spuCache.put("spu" + i, "data" + i);
        }
        redisMessageListenerContainer.addMessageListener(redisCacheMsgListener, new ChannelTopic("cache"));
        log.info("结束加载系统缓存...");
    }

}
