package com.ww.mall.netty.config;

import com.ww.mall.netty.handler.chat.*;
import com.ww.mall.netty.properties.MallNettyProperties;
import com.ww.mall.netty.protocol.MallProtocolFrameDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
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
                //使用指定的端口设置套接字地址
                .localAddress(new InetSocketAddress(mallNettyProperties.getPort()))
                //服务端可连接队列数,对应TCP/IP协议listen函数中backlog参数
                .option(ChannelOption.SO_BACKLOG, 1024)
                //设置TCP长连接,一般如果两个小时内没有数据的通信时,TCP会自动发送一个活动探测数据报文
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                //将小的数据包包装成更大的帧进行传送，提高网络的负载,即TCP延迟传输
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new MallProtocolFrameDecoder());
                        ch.pipeline().addLast(messageCodecHandler);
                        ch.pipeline().addLast(new IdleStateHandler(5, 0, 0));
                        ch.pipeline().addLast(new ChannelDuplexHandler() {
                            // 用来触发特殊事件
                            @Override
                            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                IdleStateEvent event = (IdleStateEvent) evt;
                                // 触发了读空闲事件
                                if (event.state() == IdleState.READER_IDLE) {
                                    log.debug("已经 5s 没有读到数据了");
                                    ctx.channel().close();
                                }
                            }
                        });
                        ch.pipeline().addLast(loginRequestMessageHandler);
                        ch.pipeline().addLast(chatRequestMessageHandler);
                        ch.pipeline().addLast(groupCreateRequestMessageHandler);
                        ch.pipeline().addLast(groupJoinRequestMessageHandler);
                        ch.pipeline().addLast(groupMembersRequestMessageHandler);
                        ch.pipeline().addLast(groupQuitRequestMessageHandler);
                        ch.pipeline().addLast(groupChatRequestMessageHandler);
                        ch.pipeline().addLast(chatQuitHandler);
                    }
                });
        ChannelFuture channelFuture = serverBootstrap.bind().sync();
        if (channelFuture.isSuccess()) {
            log.info("netty server success start");
        } else {
            log.error("netty server fail start");
        }
    }

    @PreDestroy
    public void destroy() throws InterruptedException {
        boss.shutdownGracefully().sync();
        work.shutdownGracefully().sync();
        log.info("关闭Netty server");
    }

}
