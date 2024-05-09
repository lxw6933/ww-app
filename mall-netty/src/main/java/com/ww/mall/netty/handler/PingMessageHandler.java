package com.ww.mall.netty.handler;

import com.ww.mall.netty.handler.chat.MallAbstractChatInboundHandler;
import com.ww.mall.netty.holder.ClientSocketHolder;
import com.ww.mall.netty.message.chat.PingChatMessage;
import com.ww.mall.netty.message.chat.PongChatMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ww
 * @create 2024-05-09- 10:21
 * @description:
 */
@Slf4j
@ChannelHandler.Sharable
public class PingMessageHandler extends MallAbstractChatInboundHandler<PingChatMessage> {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt ;
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                log.info("已经30秒没有收到客户端信息！");
                PongChatMessage pongChatMessage = new PongChatMessage();
                pongChatMessage.setSequenceId(ctx.channel().hashCode());
                ctx.writeAndFlush(pongChatMessage);
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PingChatMessage msg) throws Exception {
        log.info("【{}】服务端收到客户端心跳消息：{}", ctx.channel(), msg);
        ClientSocketHolder.put((long) msg.getSequenceId(), (NioSocketChannel) ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端渠道无效，移除无效客户端{}", ctx.channel());
        ClientSocketHolder.removeClientSocket((NioSocketChannel) ctx.channel());
    }
}
