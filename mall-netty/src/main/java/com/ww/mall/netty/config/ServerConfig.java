package com.ww.mall.netty.config;

import com.ww.mall.netty.handler.ServerHandlerInitializer;
import com.ww.mall.netty.handler.chat.*;
import com.ww.mall.netty.properties.MallNettyProperties;
import com.ww.mall.netty.protocol.MallProtocolFrameDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.net.InetSocketAddress;

/**
 * @author ww
 * @create 2024-05-08- 13:52
 * @description:
 */
@Slf4j
@Component
public class ServerConfig {

    private final EventLoopGroup boss = new NioEventLoopGroup();
    private final EventLoopGroup work = new NioEventLoopGroup();

    @Resource
    private MallNettyProperties mallNettyProperties;

    @Resource
    private MessageCodecHandler messageCodecHandler;
    @Resource
    private LoginRequestMessageHandler loginRequestMessageHandler;
    @Resource
    private ChatRequestMessageHandler chatRequestMessageHandler;
    @Resource
    private GroupCreateRequestMessageHandler groupCreateRequestMessageHandler;
    @Resource
    private GroupJoinRequestMessageHandler groupJoinRequestMessageHandler;
    @Resource
    private GroupMembersRequestMessageHandler groupMembersRequestMessageHandler;
    @Resource
    private GroupQuitRequestMessageHandler groupQuitRequestMessageHandler;
    @Resource
    private GroupChatRequestMessageHandler groupChatRequestMessageHandler;
    @Resource
    private ChatQuitHandler chatQuitHandler;

    @PostConstruct
    public void start() throws InterruptedException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(boss, work)
                .channel(NioServerSocketChannel.class)
                // 使用指定的端口设置套接字地址
                .localAddress(new InetSocketAddress(mallNettyProperties.getPort()))
                .childHandler(new ServerHandlerInitializer());
        ChannelFuture channelFuture = serverBootstrap.bind().sync();
        if (channelFuture.isSuccess()) {
            log.info("netty server success start");
        } else {
            log.error("netty server fail start");
        }
        ChannelFuture serverChannelFuture = channelFuture.channel().closeFuture();
        serverChannelFuture.addListener((ChannelFutureListener) channelFuture1 -> log.info("服务端渠道关闭"));
    }

    @PreDestroy
    public void destroy() throws InterruptedException {
        boss.shutdownGracefully().sync();
        work.shutdownGracefully().sync();
        log.info("关闭Netty server");
    }

}
