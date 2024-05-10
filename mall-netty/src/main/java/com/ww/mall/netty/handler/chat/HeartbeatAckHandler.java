package com.ww.mall.netty.handler.chat;

import com.ww.mall.netty.message.chat.HeartbeatAckMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-05-09 22:27
 * @description:
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class HeartbeatAckHandler extends MallAbstractChatInboundHandler<HeartbeatAckMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HeartbeatAckMessage ack) throws Exception {
        log.info("[client] receive the server heartbeat message ack");
    }

}
