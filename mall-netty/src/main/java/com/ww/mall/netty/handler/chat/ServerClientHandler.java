package com.ww.mall.netty.handler.chat;

import com.ww.mall.netty.holder.ClientSocketHolder;
import com.ww.mall.netty.service.SessionService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author ww
 * @create 2024-05-07 22:02
 * @description:
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class ServerClientHandler extends ChannelInboundHandlerAdapter {

    @Resource
    private SessionService sessionService;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("客户端消息【{}】", msg);
        super.channelRead(ctx, msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String clientIp = getClientIp(ctx);
        int clientPort = getClientPort(ctx);
        ClientSocketHolder.put(ctx.channel().id().asLongText(), (NioSocketChannel) ctx.channel());
        log.info("有新客户端【{}:{}】建立连接, 目前客户端连接数：{}", clientIp, clientPort, ClientSocketHolder.getAllClientSocket().size());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        sessionService.unbind(ctx.channel());
        ClientSocketHolder.removeClientSocket((NioSocketChannel) ctx.channel());
        String clientIp = getClientIp(ctx);
        int clientPort = getClientPort(ctx);
        log.info("客户端【{}:{}】断开连接", clientIp, clientPort);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        sessionService.unbind(ctx.channel());
        String clientIp = getClientIp(ctx);
        int clientPort = getClientPort(ctx);
        log.info("客户端【{}:{}】出现异常：{}", clientIp, clientPort, cause.getMessage());
    }

    private String getClientIp(ChannelHandlerContext ctx) {
        // 获取客户端的IP地址和端口
        SocketAddress clientSocketAddress = ctx.channel().remoteAddress();
        // 从客户端地址中解析出IP地址
        return ((InetSocketAddress) clientSocketAddress).getAddress().getHostAddress();
    }

    private int getClientPort(ChannelHandlerContext ctx) {
        // 获取客户端的IP地址和端口
        SocketAddress clientSocketAddress = ctx.channel().remoteAddress();
        // 从客户端地址中解析出IP地址
        return ((InetSocketAddress) clientSocketAddress).getPort();
    }

}
