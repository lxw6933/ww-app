package com.ww.mall.redis;

import org.springframework.data.redis.connection.MessageListener;

/**
 * @author ww
 * @create 2024-04-11- 13:37
 * @description:
 */
public abstract class MallRedisListener implements MessageListener {

    abstract protected String channelName();

}
