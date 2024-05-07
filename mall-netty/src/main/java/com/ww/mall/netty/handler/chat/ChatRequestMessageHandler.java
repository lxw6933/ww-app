package com.ww.mall.netty.handler.chat;

import com.ww.mall.netty.message.chat.req.ChatRequestMessage;
import com.ww.mall.netty.message.chat.res.ChatResponseMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-05-07 21:45
 * @description:
 */
@Component
@ChannelHandler.Sharable
public class ChatRequestMessageHandler extends MallAbstractChatInboundHandler<ChatRequestMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatRequestMessage msg) throws Exception {
        String to = msg.getTo();
        Channel channel = sessionService.getChannel(to);
        // 在线
        if (channel != null) {
            channel.writeAndFlush(new ChatResponseMessage(msg.getFrom(), msg.getContent()));
        }
        // 不在线
        else {
            ctx.writeAndFlush(new ChatResponseMessage(false, "对方用户不存在或者不在线"));
        }
    }
}
