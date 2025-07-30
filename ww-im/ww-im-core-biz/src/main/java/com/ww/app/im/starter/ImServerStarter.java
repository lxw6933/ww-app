package com.ww.app.im.starter;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.ww.app.common.exception.ApiException;
import com.ww.app.im.configuration.ImProperties;
import com.ww.app.im.handler.initializer.ImServerHandlerInitializer;
import com.ww.app.im.utils.ImChannelHandlerContextUtils;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    public void destroy() {
        log.info("开始关闭IM服务器...");
        try {
            // 设置较短的超时时间，避免阻塞太久
            long timeoutMs = 5000;
            
            // 异步关闭bossGroup
            CompletableFuture<Void> bossFuture = CompletableFuture.runAsync(() -> {
                try {
                    bossGroup.shutdownGracefully().await(timeoutMs, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("关闭bossGroup时被中断");
                } catch (Exception e) {
                    log.warn("关闭bossGroup时发生异常", e);
                }
            });
            
            // 异步关闭workerGroup
            CompletableFuture<Void> workerFuture = CompletableFuture.runAsync(() -> {
                try {
                    workerGroup.shutdownGracefully().await(timeoutMs, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("关闭workerGroup时被中断");
                } catch (Exception e) {
                    log.warn("关闭workerGroup时发生异常", e);
                }
            });
            
            // 等待两个Group都关闭完成，但设置总超时
            CompletableFuture.allOf(bossFuture, workerFuture).get(timeoutMs * 2, TimeUnit.MILLISECONDS);
            
            log.info("IM服务器关闭完成");
        } catch (TimeoutException e) {
            log.warn("IM服务器关闭超时，强制关闭");
            // 强制关闭
            if (!bossGroup.isShutdown()) {
                bossGroup.shutdownGracefully();
            }
            if (!workerGroup.isShutdown()) {
                workerGroup.shutdownGracefully();
            }
        } catch (Exception e) {
            log.error("关闭IM服务器时发生异常", e);
        }
    }
}
