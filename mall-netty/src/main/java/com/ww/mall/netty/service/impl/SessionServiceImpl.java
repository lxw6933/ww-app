package com.ww.mall.netty.service.impl;

import com.ww.mall.netty.service.SessionService;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ww
 * @create 2024-05-07 21:36
 * @description: 会话实现
 */
@Slf4j
@Service
public class SessionServiceImpl implements SessionService {
    /**
     * 用户渠道绑定
     */
    private final Map<String, Channel> usernameChannelMap = new ConcurrentHashMap<>();

    /**
     * 渠道用户绑定
     */
    private final Map<Channel, String> channelUsernameMap = new ConcurrentHashMap<>();

    /**
     * 渠道属性绑定
     */
    private final Map<Channel, Map<String, Object>> channelAttributesMap = new ConcurrentHashMap<>();

    @Override
    public void bind(Channel channel, String username) {
        usernameChannelMap.put(username, channel);
        channelUsernameMap.put(channel, username);
        channelAttributesMap.put(channel, new ConcurrentHashMap<>());
    }

    @Override
    public void unbind(Channel channel) {
        String username = channelUsernameMap.remove(channel);
        if (StringUtils.isNoneEmpty(username)) {
            usernameChannelMap.remove(username);
            channelAttributesMap.remove(channel);
        }
    }

    @Override
    public Object getAttribute(Channel channel, String name) {
        return channelAttributesMap.get(channel).get(name);
    }

    @Override
    public void setAttribute(Channel channel, String name, Object value) {
        channelAttributesMap.get(channel).put(name, value);
    }

    @Override
    public Channel getChannel(String username) {
        return usernameChannelMap.get(username);
    }

    @Override
    public String toString() {
        return usernameChannelMap.toString();
    }
}
