package com.ww.mall.netty.handler.chat;

import com.ww.mall.netty.message.chat.req.GroupChatRequestMessage;
import com.ww.mall.netty.message.chat.res.GroupChatResponseMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author ww
 * @create 2024-05-07 21:52
 * @description:
 */
@Component
@ChannelHandler.Sharable
public class GroupChatRequestMessageHandler extends MallAbstractChatInboundHandler<GroupChatRequestMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupChatRequestMessage msg) throws Exception {
        List<Channel> channels = groupSessionService.getMembersChannel(msg.getGroupName());

        for (Channel channel : channels) {
            channel.writeAndFlush(new GroupChatResponseMessage(msg.getFrom(), msg.getContent()));
        }
    }
}
