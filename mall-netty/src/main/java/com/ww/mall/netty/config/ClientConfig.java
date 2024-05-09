package com.ww.mall.netty.config;

import com.ww.mall.netty.handler.ClientHandlerInitializer;
import com.ww.mall.netty.handler.chat.MessageCodecHandler;
import com.ww.mall.netty.message.chat.MallChatMessage;
import com.ww.mall.netty.properties.MallNettyProperties;
import com.ww.mall.netty.protocol.MallProtocolFrameDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author ww
 * @create 2024-05-08- 13:52
 * @description:
 */
@Slf4j
@Component
public class ClientConfig {

    private SocketChannel socketChannel;

    private final EventLoopGroup group = new NioEventLoopGroup();

    @Resource
    private MallNettyProperties mallNettyProperties;

    public void sendMsg(MallChatMessage message) {
        socketChannel.writeAndFlush(message);
    }

    @PostConstruct
    public void start() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .remoteAddress("127.0.0.1", mallNettyProperties.getPort())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ClientHandlerInitializer());
            ChannelFuture future = bootstrap.connect();
        //客户端断线重连逻辑
        future.addListener((ChannelFutureListener) channelFuture -> {
            if (channelFuture.isSuccess()) {
                log.info("连接Netty服务端成功");
            } else {
                log.info("连接失败，进行断线重连");
                channelFuture.channel().eventLoop().schedule(this::start, 20, TimeUnit.SECONDS);
            }
        });
        socketChannel = (SocketChannel) future.channel();
    }

}
