package com.ww.mall.netty.handler.chat;

import cn.hutool.extra.spring.SpringUtil;
import com.ww.mall.netty.config.ClientConfig;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2024-05-07 22:02
 * @description:
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class ClientHandler extends ChannelInboundHandlerAdapter {

    @Value("${server.port}")
    private int serverPort;

    @Value("${spring.application.name}")
    private String serviceName;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("[client] receive netty server msg:【{}】", msg);
        super.channelRead(ctx, msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        log.info("发送服务名、端口和 IP 地址等信息给服务端");
//        String message = serviceName + "," + serverPort;
//        ctx.writeAndFlush(ctx.alloc().buffer().writeBytes(message.getBytes()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.warn("[client] disconnect netty server, 3s after try to reconnect");
        ClientConfig clientConfig = SpringUtil.getBean(ClientConfig.class);
        ctx.channel().eventLoop().schedule(clientConfig::start, 3, TimeUnit.SECONDS);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("[client] channel exception：{}", cause.getMessage());
        super.exceptionCaught(ctx, cause);
    }

}
