package com.ww.mall.netty.handler;

import com.ww.mall.netty.handler.chat.MallAbstractChatInboundHandler;
import com.ww.mall.netty.holder.ClientSocketHolder;
import com.ww.mall.netty.message.chat.HeartbeatAckMessage;
import com.ww.mall.netty.message.chat.PingChatMessage;
import com.ww.mall.netty.message.chat.PongChatMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.nio.NioSocketChannel;
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
public class PingMessageHandler extends MallAbstractChatInboundHandler<PingChatMessage> {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt ;
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                log.info("[server] 15 seconds did not receive the client ❤ message! Disconnect the client[{}]", ctx.channel().remoteAddress());
                ctx.channel().close();
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PingChatMessage msg) throws Exception {
        log.info("[server] the server receives a ❤ message from the client[{}]", ctx.channel().remoteAddress());
        // 给客户端ack响应
        ctx.writeAndFlush(new HeartbeatAckMessage());
    }

}
