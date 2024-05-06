package com.ww.mall.netty.config;

import com.ww.mall.netty.handler.MallWebSocketHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-05-06- 16:53
 * @description:
 */
@Configuration
public class MallNettyWebSocketConfig {

    @Resource
    private MallWebSocketHandler mallWebSocketHandler;

    @Bean
    public EventLoopGroup bossGroup() {
        return new NioEventLoopGroup();
    }

    @Bean
    public EventLoopGroup workerGroup() {
        return new NioEventLoopGroup();
    }

    @Bean
    public ServerBootstrap serverBootstrap() {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup(), workerGroup())
                .channel(NioServerSocketChannel.class)
                // 设置服务器端TCP连接队列的大小。即服务器端等待接受客户端连接的队列的最大长度。默认值为 128
                .option(ChannelOption.SO_BACKLOG, 128)
                // 设置TCP连接是否启用心跳保活机制。如果启用，操作系统会定期发送心跳包以检测连接是否仍然活跃
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        // http编解码器
                        pipeline.addLast(new HttpServerCodec());
                        // 将HTTP消息的多个部分合并为一个完整的FullHttpRequest或者FullHttpResponse。在WebSocket协议的握手阶段，可以将多个HTTP消息部分合并为一个完整的握手请求
                        pipeline.addLast(new HttpObjectAggregator(65536));
                        // 处理WebSocket握手请求和处理WebSocket协议的帧。它负责处理WebSocket的握手请求，并将请求升级为WebSocket连接
                        pipeline.addLast(new WebSocketServerProtocolHandler("/mall-websocket"));
                        // 处理来自客户端的WebSocket消息，并向客户端发送WebSocket消息
                        pipeline.addLast(mallWebSocketHandler);
                    }
                });
        return serverBootstrap;
    }

    @PreDestroy
    public void destroy() throws InterruptedException {
        bossGroup().shutdownGracefully().sync();
        workerGroup().shutdownGracefully().sync();
    }

}
