package com.ww.mall.netty.handler;

import com.ww.mall.netty.handler.chat.MallAbstractChatInboundHandler;
import com.ww.mall.netty.message.chat.PingChatMessage;
import com.ww.mall.netty.message.chat.PongChatMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-05-09- 10:21
 * @description:
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class PongMessageHandler extends MallAbstractChatInboundHandler<PongChatMessage> {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt ;
            if (idleStateEvent.state() == IdleState.WRITER_IDLE){
                log.info("已经10秒没有给服务端发送信息！");
                PingChatMessage pingChatMessage = new PingChatMessage();
                pingChatMessage.setSequenceId(ctx.channel().hashCode());
                ctx.writeAndFlush(pingChatMessage);
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PongChatMessage msg) throws Exception {
        log.info("【{}】客户端收到服务端心跳回应消息：{}", ctx.channel(), msg);
        // 给客户端发送心跳消息
        PingChatMessage pingChatMessage = new PingChatMessage();
        pingChatMessage.setSequenceId(ctx.channel().hashCode());
        ctx.writeAndFlush(pingChatMessage);
    }

}
