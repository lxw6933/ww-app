package com.ww.mall.im.starter;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.im.configuration.ImProperties;
import com.ww.mall.im.handler.initializer.ImServerHandlerInitializer;
import com.ww.mall.im.utils.ImChannelHandlerContextUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-11-09 14:56
 * @description: IM server
 */
@Slf4j
@Configuration
public class ImServerStarter implements InitializingBean {

    @Resource
    private ImProperties imProperties;

    @Resource
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    @Resource
    private ImServerHandlerInitializer imServerHandlerInitializer;

    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    public void start() {
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.group(bossGroup, workerGroup);
            serverBootstrap.childHandler(imServerHandlerInitializer);

            ChannelFuture serverChannelStartedFuture;
            try {
                serverChannelStartedFuture = serverBootstrap.bind(imProperties.getPort()).sync();
                if (serverChannelStartedFuture.isSuccess()) {
                    // 记录当前im server ip
                    ImChannelHandlerContextUtils.setImServerIp(nacosDiscoveryProperties.getIp());
                    log.info("[im server] success start port：[{}]", imProperties.getPort());
                } else {
                    log.error("[im server] fail start");
                }
                ChannelFuture serverChannelCloseFuture = serverChannelStartedFuture.channel().closeFuture();
                serverChannelCloseFuture.addListener((ChannelFutureListener) channelFuture ->
                    log.error("[im server] channel closed")
                ).sync();
            } catch (Exception e) {
                throw new ApiException("[im server] exception start");
            }
        } catch (Exception e) {
            log.error("[im server] exception: {}", e.getMessage(), e);
            throw new ApiException("[im server] error");
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

//    @Resource
//    private ImClientStarter imClientStarter;

    @Override
    public void afterPropertiesSet() {
        new Thread(this::start, "im server").start();
//        Thread.sleep(1000);
//        new Thread(() -> imClientStarter.start(), "im client").start();
    }

    @PreDestroy
    public void destroy() throws InterruptedException {
        bossGroup.shutdownGracefully().sync();
        workerGroup.shutdownGracefully().sync();
    }
}
