package com.ww.mall.product.listener;

import cn.hutool.core.collection.CollectionUtil;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ww.app.redis.listener.RedisChannelListener;
import com.ww.mall.product.cache.ProductSpuCache;
import com.ww.mall.product.constants.RedisChannelConstant;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.connection.Message;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author ww
 * @create 2025-09-12 17:36
 * @description:
 */
@Slf4j
@Component
public class CacheRedisListener extends RedisChannelListener {

    @Resource
    private LoadingCache<Long, ProductSpuCache> spuCache;

    @Override
    public List<String> channelName() {
        return CollectionUtil.toList(RedisChannelConstant.SPU_CACHE_CHANNEL);
    }

    @Override
    public void onMessage(@NotNull Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String keyStr = new String(message.getBody());
        log.info("收到订阅渠道【{}】通知【{}】数据变更", channel, keyStr);
        switch (channel) {
            case RedisChannelConstant.SPU_CACHE_CHANNEL:
                spuCache.invalidate(Long.valueOf(keyStr));
                break;
            case RedisChannelConstant.BRAND_CACHE_CHANNEL:
                break;
            default:
        }
    }
}
