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
public class ClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("收到服务端消息===================【{}】", msg);
        super.channelRead(ctx, msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("发送服务名、端口和 IP 地址等信息给服务端");
        String message = serviceName + "," + ctx.channel().remoteAddress();
        ctx.writeAndFlush(ctx.alloc().buffer().writeBytes(message.getBytes()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端无效===========================");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("客户端出现异常：{}===============================", cause.getMessage());
    }

}
