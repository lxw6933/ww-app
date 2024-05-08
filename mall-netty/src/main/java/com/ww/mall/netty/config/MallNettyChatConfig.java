package com.ww.mall.netty.config;

import com.ww.mall.netty.handler.chat.*;
import com.ww.mall.netty.protocol.MallProtocolFrameDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-05-06- 16:53
 * @description:
 */
@Slf4j
@Configuration
public class MallNettyChatConfig {

    @Bean
    public EventLoopGroup chatBossGroup() {
        return new NioEventLoopGroup();
    }

    @Bean
    public EventLoopGroup chatWorkerGroup() {
        return new NioEventLoopGroup();
    }

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

    @Bean
    public LoggingHandler loggingHandler() {
        return new LoggingHandler(LogLevel.DEBUG);
    }

    @Bean
    public ServerBootstrap chatServerBootstrap() {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(chatBossGroup(), chatWorkerGroup())
                .channel(NioServerSocketChannel.class)
                // 设置TCP连接是否启用心跳保活机制。如果启用，操作系统会定期发送心跳包以检测连接是否仍然活跃
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new MallProtocolFrameDecoder());
                        ch.pipeline().addLast(loggingHandler());
                        ch.pipeline().addLast(messageCodecHandler);
                        ch.pipeline().addLast(new IdleStateHandler(5, 0, 0));
                        ch.pipeline().addLast(new ChannelDuplexHandler() {
                            // 用来触发特殊事件
                            @Override
                            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception{
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
        return serverBootstrap;
    }

    @PreDestroy
    public void destroy() throws InterruptedException {
        chatBossGroup().shutdownGracefully().sync();
        chatWorkerGroup().shutdownGracefully().sync();
    }

}
