package com.ww.app.im.handler.server;

import com.ww.app.common.exception.ApiException;
import com.ww.app.im.common.ImMsg;
import com.ww.app.im.component.ImHandlerComponent;
import com.ww.app.im.handler.msg.LogoutMsgHandlerAdapter;
import com.ww.app.im.utils.ImContextUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-11-10 16:25
 * @description: im msg handler
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class ImMsgServerHandler extends SimpleChannelInboundHandler<Object> {

    @Resource
    protected ImHandlerComponent imHandlerComponent;

    @Resource
    private LogoutMsgHandlerAdapter logoutMsgHandlerAdapter;

    @Override
    public void channelRead0(ChannelHandlerContext channelHandlerContext, Object imMsg) {
        if (imMsg instanceof ImMsg) {
            imHandlerComponent.handle(channelHandlerContext, (ImMsg) imMsg);
        } else {
            throw new ApiException("消息异常");
        }
    }

    /**
     * 正常、异常断线都会触发
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("客户端断开连接: {}", ctx.channel());
        Long userId = ImContextUtils.getUserId(ctx);
        Integer appId = ImContextUtils.getAppId(ctx);
        if (userId != null && appId != null) {
            logoutMsgHandlerAdapter.logoutHandler(ctx, userId, appId);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端建立连接: {}", ctx.channel());
    }
}
