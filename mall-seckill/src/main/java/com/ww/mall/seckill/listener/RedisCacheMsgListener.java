package com.ww.mall.seckill.listener;

import cn.hutool.core.collection.CollectionUtil;
import com.ww.mall.common.constant.RedisChannelConstant;
import com.ww.mall.redis.MallRedisListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.stereotype.Component;

import java.util.List;

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
        switch (channel) {
            case RedisChannelConstant.MALL_SPU_CHANNEL:
                break;
            case RedisChannelConstant.MALL_SKU_CHANNEL:
                break;
            case RedisChannelConstant.MALL_SMS_CHANNEL:
                break;
            default:
        }
    }

    @Override
    protected List<String> channelName() {
        return CollectionUtil.toList(RedisChannelConstant.MALL_SPU_CHANNEL, RedisChannelConstant.MALL_SKU_CHANNEL, RedisChannelConstant.MALL_SMS_CHANNEL);
    }
}
