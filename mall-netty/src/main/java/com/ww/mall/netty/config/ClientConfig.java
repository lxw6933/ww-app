package com.ww.mall.netty.config;

import com.ww.mall.netty.handler.ClientHandlerInitializer;
import com.ww.mall.netty.message.chat.MallChatMessage;
import com.ww.mall.netty.properties.MallNettyProperties;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

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

    @Resource
    private ClientHandlerInitializer clientHandlerInitializer;

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
                .handler(clientHandlerInitializer);
            ChannelFuture future = bootstrap.connect();
        // 客户端连接服务端逻辑
        future.addListener((ChannelFutureListener) channelFuture -> {
            if (channelFuture.isSuccess()) {
                log.info("connect netty server success");
            } else {
                log.warn("connect netty server fail, 3s after try to reconnect");
                channelFuture.channel().eventLoop().schedule(this::start, 3, TimeUnit.SECONDS);
            }
        });
        socketChannel = (SocketChannel) future.channel();
    }

}
