package com.ww.mall.netty.service;

import io.netty.channel.Channel;

/**
 * @author ww
 * @create 2024-05-07 21:35
 * @description: 会话管理
 */
public interface SessionService {

    /**
     * 绑定用户会话
     *
     * @param channel  渠道
     * @param username 用户
     */
    void bind(Channel channel, String username);

    /**
     * 解绑会话
     *
     * @param channel 渠道
     */
    void unbind(Channel channel);

    /**
     * 获取属性
     *
     * @param channel 渠道
     * @param name    属性名
     * @return 属性值
     */
    Object getAttribute(Channel channel, String name);

    /**
     * 设置属性
     *
     * @param channel 渠道
     * @param name    属性名
     * @param value   属性值
     */
    void setAttribute(Channel channel, String name, Object value);

    /**
     * 根据用户名获取 channel
     *
     * @param username 用户名
     * @return channel
     */
    Channel getChannel(String username);

}
