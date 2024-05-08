package com.ww.mall.netty.config;

import com.ww.mall.common.exception.ApiException;
import com.ww.mall.netty.handler.chat.*;
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
import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-05-08- 13:52
 * @description:
 */
@Slf4j
@Component
public class ServerConfig {

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
    public void init() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
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
            Channel channel;
            try {
                channel = serverBootstrap.bind(9000).sync().channel();
            } catch (Exception e) {
                log.error("server bind error", e);
                throw new ApiException(e);
            }
            channel.closeFuture().addListener((ChannelFutureListener) listener -> {
                if (listener.isSuccess()) {
                    // Channel 关闭成功时执行的逻辑
                    System.out.println("服务端关闭 successfully.");
                } else {
                    // Channel 关闭失败时执行的逻辑
                    log.error("监听netty 【server】 close {}", listener.cause().getMessage());
                    throw new ApiException("======server close========");
                }
            });
            log.info("===================server=======================");
        } catch (Exception e) {
            log.error("服务端异常：", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
