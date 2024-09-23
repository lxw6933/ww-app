package com.ww.mall.netty.config;

import com.ww.mall.netty.handler.ServerHandlerInitializer;
import com.ww.mall.netty.properties.MallNettyProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
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
    private ServerHandlerInitializer serverHandlerInitializer;

    @PostConstruct
    public void start() throws InterruptedException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(boss, work)
                .channel(NioServerSocketChannel.class)
                // 使用指定的端口设置套接字地址
                .localAddress(new InetSocketAddress(mallNettyProperties.getPort()))
                .childHandler(serverHandlerInitializer);
        ChannelFuture serverStartFuture = serverBootstrap.bind().sync();
        if (serverStartFuture.isSuccess()) {
            log.info("netty server success start port：[{}]", mallNettyProperties.getPort());
        } else {
            log.error("netty server fail start");
        }
        ChannelFuture serverChannelFuture = serverStartFuture.channel().closeFuture();
        serverChannelFuture.addListener((ChannelFutureListener) channelFuture ->
                log.warn("netty server channel close")
        );
    }

    @PreDestroy
    public void destroy() throws InterruptedException {
        boss.shutdownGracefully();
        work.shutdownGracefully();
        log.info("netty server close success");
    }

}
