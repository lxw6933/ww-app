package com.ww.mall.redis.listener;

import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ww
 * @create 2024-04-11- 13:37
 * @description:
 */
public abstract class MallRedisListener implements MessageListener {

    abstract public List<String> channelName();

    public List<ChannelTopic> channelTopics() {
        List<ChannelTopic> channelTopics = new ArrayList<>();
        this.channelName().forEach(channelName -> channelTopics.add(new ChannelTopic(channelName)));
        return channelTopics;
    }

}
