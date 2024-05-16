package com.ww.mall.netty.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ww
 * @create 2024-05-06- 16:43
 * @description: netty websocket handler
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class MallWebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    /**
     * 管理所有客户端的channel通道
     */
    public static ChannelGroup clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 用户id和channelId绑定
     */
    private static final Map<String, Channel> managerChannel = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        // 接收到客户端发送的消息
        String content = msg.text();
        log.info("Received client message: {}", content);
        // 假设这里有一个业务逻辑处理过程
        String responseMessage = "server processed: " + content;
        // 发送消息给客户端
        ctx.channel().writeAndFlush(new TextWebSocketFrame(responseMessage));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        clients.add(ctx.channel());
        log.info("客户端建立连接，通道开启！id={},localAddress={},remoteAddress={}", ctx.channel().id(), ctx.channel().localAddress(), ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        ctx.close();
        clients.remove(channel);
        remove(channel);
        log.info("客户端断开连接，通道关闭！id={},localAddress={},remoteAddress={}", ctx.channel().id(), ctx.channel().localAddress(), ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 异常处理
        Channel channel = ctx.channel();
        ctx.close();
        clients.remove(channel);
        remove(channel);
        log.error("客户端异常，通道关闭！id={},localAddress={},remoteAddress={}", channel.id(), channel.localAddress(), channel.remoteAddress());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            log.info("client handler websocket server");
            WebSocketServerProtocolHandler.HandshakeComplete complete = (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            String uri = complete.requestUri();
            String token = getToken(uri);
//            if (StringUtils.isEmpty(token)) {
//                ctx.channel().close();
//                return;
//            }
            log.info("client request uri: {}", uri);
        }
    }

    private String getToken(String uri) {
        if (StringUtils.isEmpty(uri) || !uri.contains("?")) {
            return null;
        }
        String[] queryParams = uri.split("\\?");
        if (queryParams.length != 2) {
            return null;
        }
        String[] params = queryParams[1].split("=");
        if (params.length != 2) {
            return null;
        }
        return params[1];
    }

    /**
     * 移除Channel
     */
    public static void remove(Channel channel) {
        for (Map.Entry<String, Channel> entry : managerChannel.entrySet()) {
            if (entry.getValue().equals(channel)) {
                managerChannel.remove(entry.getKey());
            }
        }
    }

}
