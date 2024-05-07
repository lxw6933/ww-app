package com.ww.mall.netty.handler.chat;

import com.ww.mall.netty.service.GroupSessionService;
import com.ww.mall.netty.service.SessionService;
import com.ww.mall.netty.service.UserService;
import io.netty.channel.SimpleChannelInboundHandler;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-05-07 21:53
 * @description:
 */
public abstract class MallAbstractChatInboundHandler<T> extends SimpleChannelInboundHandler<T> {

    @Resource
    protected GroupSessionService groupSessionService;

    @Resource
    protected SessionService sessionService;

    @Resource
    protected UserService userService;

}
