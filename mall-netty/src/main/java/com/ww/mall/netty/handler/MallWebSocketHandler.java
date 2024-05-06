package com.ww.mall.netty.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-05-06- 16:43
 * @description: netty websocket handler
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class MallWebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        // 接收到客户端发送的消息
        String receivedMessage = msg.text();
        log.info("Received client message: {}", receivedMessage);
        // 假设这里有一个业务逻辑处理过程
        String responseMessage = "server processed: " + receivedMessage;
        // 发送消息给客户端
        ctx.channel().writeAndFlush(new TextWebSocketFrame(responseMessage));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端建立连接，通道开启！id={},localAddress={},remoteAddress={}", ctx.channel().id(), ctx.channel().localAddress(), ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端断开连接，通道关闭！id={},localAddress={},remoteAddress={}", ctx.channel().id(), ctx.channel().localAddress(), ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 异常处理
        log.error("客户端异常，通道关闭！id={},localAddress={},remoteAddress={}", ctx.channel().id(), ctx.channel().localAddress(), ctx.channel().remoteAddress());
        ctx.close();
    }

}
