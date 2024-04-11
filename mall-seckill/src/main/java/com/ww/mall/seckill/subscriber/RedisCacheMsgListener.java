package com.ww.mall.seckill.subscriber;

import com.ww.mall.common.constant.RedisChannelConstant;
import com.ww.mall.redis.MallRedisListener;
import com.ww.mall.seckill.manager.CacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-04-11- 09:33
 * @description: redis消息订阅者
 */
@Slf4j
@Component
public class RedisCacheMsgListener extends MallRedisListener {

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String content = new String(message.getChannel());
        log.info("接收到redis渠道【{}】: 发布的内容【{}】", channel, content);
        CacheManager.spuCache.asMap().forEach((key, value) -> log.info("key：【{}】value：【{}】", key, value));
        System.out.println("========================");
        CacheManager.spuCache.invalidate("spu" + message);
        CacheManager.spuCache.asMap().forEach((key, value) -> log.info("key：【{}】value：【{}】", key, value));
    }

    @Override
    protected String channelName() {
        return RedisChannelConstant.MALL_SPU_CHANNEL;
    }
}
