package com.ww.mall.netty.holder;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-05-13 21:57
 * @description:
 */
public class WebSocketClientChannelHolder {

    public static void addContext(String userId, Channel channel) {
        String channelId = channel.id().toString();
        AttributeKey attributeKey;
        if (!AttributeKey.exists(channelId)) {
            attributeKey = AttributeKey.newInstance(channelId);
        } else {
            attributeKey = AttributeKey.valueOf(channelId);
        }
        channel.attr(attributeKey).set(userId);
    }

}
