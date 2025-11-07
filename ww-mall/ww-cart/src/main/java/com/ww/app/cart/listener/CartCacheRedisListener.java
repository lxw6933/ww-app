package com.ww.app.cart.listener;

import cn.hutool.core.collection.CollectionUtil;
import com.ww.app.cart.component.CartCacheComponent;
import com.ww.app.common.constant.RedisChannelConstant;
import com.ww.app.redis.listener.RedisChannelListener;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.connection.Message;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author ww
 * @create 2025-11-07 10:57
 * @description:
 */
@Slf4j
@Component
public class CartCacheRedisListener extends RedisChannelListener {

    @Resource
    private CartCacheComponent cartCacheComponent;

    @Override
    public List<String> channelName() {
        return CollectionUtil.toList(RedisChannelConstant.USER_CART_CHANNEL);
    }

    @Override
    public void onMessage(@NotNull Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String keyStr = new String(message.getBody());
        log.info("收到订阅渠道【{}】通知【{}】数据变更", channel, keyStr);
        switch (channel) {
            case RedisChannelConstant.USER_CART_CHANNEL:
                cartCacheComponent.invalidateCache(Long.valueOf(keyStr));
                break;
            default:
        }
    }

}
