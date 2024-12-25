package com.ww.mall.im.handler.server;

import com.ww.mall.common.exception.ApiException;
import com.ww.mall.im.common.ImMsg;
import com.ww.mall.im.handler.component.ImHandlerComponent;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
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
public class ImMsgServerHandler extends ChannelInboundHandlerAdapter {

    @Resource
    protected ImHandlerComponent imHandlerComponent;

    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object imMsg) {
        if (imMsg instanceof ImMsg) {
            imHandlerComponent.handle(channelHandlerContext, (ImMsg) imMsg);
        } else {
            throw new ApiException("消息异常");
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端断开连接: {}", ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端建立连接: {}", ctx.channel());
        super.channelActive(ctx);
    }
}
