package com.ww.mall.netty.config;

import com.ww.mall.common.exception.ApiException;
import com.ww.mall.netty.handler.HeartBeatHandler;
import com.ww.mall.netty.handler.MallWebSocketHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2024-05-06- 16:53
 * @description:
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({MallNettyProperties.class})
public class MallWebSocketServerAutoConfiguration {

    @Resource
    private MallNettyProperties mallNettyProperties;

    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    @PostConstruct
    public void start() throws InterruptedException {
        log.info("start init websocket server");
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                // 设置服务器端TCP连接队列的大小。即服务器端等待接受客户端连接的队列的最大长度。默认值为 128
                .option(ChannelOption.SO_BACKLOG, 128)
                // 设置TCP连接是否启用心跳保活机制。如果启用，操作系统会定期发送心跳包以检测连接是否仍然活跃
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                // 设置监听端口
                .localAddress(mallNettyProperties.getWebsocketPort())
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        // http编解码器
                        pipeline.addLast(new HttpServerCodec());
                        // 大文件分片处理器
                        pipeline.addLast(new ChunkedWriteHandler());
                        // 将HTTP消息的多个部分合并为一个完整的FullHttpRequest或者FullHttpResponse。在WebSocket协议的握手阶段，可以将多个HTTP消息部分合并为一个完整的握手请求
                        pipeline.addLast(new HttpObjectAggregator(64 * 1024));
                        // 增加心跳检测机制[针对客户端，如果在1分钟时没有向服务端发送读写心跳(ALL)，则主动断开]
                        pipeline.addLast(new IdleStateHandler(15, 0, 0, TimeUnit.SECONDS));
                        // 自定义心跳检测处理器
                        pipeline.addLast(new HeartBeatHandler());
                        // 处理WebSocket握手请求和处理WebSocket协议的帧。它负责处理WebSocket的握手请求，并将请求升级为WebSocket连接
                        pipeline.addLast(new WebSocketServerProtocolHandler(mallNettyProperties.getWebsocketPath(), "WebSocket", true, 65536 * 10));
                        // 处理来自客户端的WebSocket消息，并向客户端发送WebSocket消息
                        pipeline.addLast(new MallWebSocketHandler());
                    }
                });
        ChannelFuture serverStartFuture;
        try {
            serverStartFuture = serverBootstrap.bind(mallNettyProperties.getWebsocketPort()).sync();
            if (serverStartFuture.isSuccess()) {
                log.info("mall websocket server success start port：【{}】", mallNettyProperties.getWebsocketPort());
            } else {
                log.error("mall websocket server fail start");
            }
            ChannelFuture serverChannelFuture = serverStartFuture.channel().closeFuture();
            serverChannelFuture.addListener((ChannelFutureListener) channelFuture ->
                log.warn("mall websocket server channel close")
            );
        } catch (Exception e) {
            throw new ApiException("mall websocket server exception start");
        }
    }

    @PreDestroy
    public void destroy() throws InterruptedException {
        bossGroup.shutdownGracefully().sync();
        workerGroup.shutdownGracefully().sync();
    }

}
