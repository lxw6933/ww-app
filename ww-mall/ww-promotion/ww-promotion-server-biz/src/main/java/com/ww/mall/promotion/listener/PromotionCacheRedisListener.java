package com.ww.mall.promotion.listener;

import cn.hutool.core.collection.CollectionUtil;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ww.app.redis.listener.RedisChannelListener;
import com.ww.mall.promotion.constants.RedisChannelConstant;
import com.ww.mall.promotion.entity.group.GroupActivity;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.connection.Message;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 促销活动本地缓存失效监听
 */
@Slf4j
@Component
public class PromotionCacheRedisListener extends RedisChannelListener {

    @Resource
    private LoadingCache<String, GroupActivity> groupActivityCache;

    @Override
    public List<String> channelName() {
        return CollectionUtil.toList(RedisChannelConstant.GROUP_ACTIVITY_CACHE_CHANNEL);
    }

    @Override
    public void onMessage(@NotNull Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String keyStr = new String(message.getBody());
        log.info("收到订阅渠道【{}】通知【{}】数据变更", channel, keyStr);
        switch (channel) {
            case RedisChannelConstant.GROUP_ACTIVITY_CACHE_CHANNEL:
                groupActivityCache.invalidate(Long.valueOf(keyStr));
                break;
            default:
        }

    }
}

