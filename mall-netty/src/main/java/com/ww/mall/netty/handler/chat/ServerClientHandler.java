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
//        ByteBuf byteBuf = (ByteBuf) msg;
//        String message = byteBuf.toString(StandardCharsets.UTF_8);
//        log.info("a client message was received:【{}】", message);
        super.channelRead(ctx, msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String clientIp = ClientSocketHolder.getClientIp(ctx);
        int clientPort = ClientSocketHolder.getClientPort(ctx);
        ClientSocketHolder.put(ctx.channel().id(), (NioSocketChannel) ctx.channel());
        log.info("[server] a new client【{}:{}】 establishes a connection, total number of current client connections：{}", clientIp, clientPort, ClientSocketHolder.getAllClientSocket().size());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        sessionService.unbind(ctx.channel());
        ClientSocketHolder.removeClientSocket(ctx.channel().id());
        String clientIp = ClientSocketHolder.getClientIp(ctx);
        int clientPort = ClientSocketHolder.getClientPort(ctx);
        log.info("[server] client【{}:{}】disconnect", clientIp, clientPort);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        sessionService.unbind(ctx.channel());
        String clientIp = ClientSocketHolder.getClientIp(ctx);
        int clientPort = ClientSocketHolder.getClientPort(ctx);
        log.info("[server] an exception[{}] occurred on the client【{}:{}】", cause.getMessage(), clientIp, clientPort);
    }

}
