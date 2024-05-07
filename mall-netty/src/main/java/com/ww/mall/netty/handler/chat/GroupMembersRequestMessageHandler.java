package com.ww.mall.netty.handler.chat;

import com.ww.mall.netty.message.chat.req.GroupMembersRequestMessage;
import com.ww.mall.netty.message.chat.res.GroupMembersResponseMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * @author ww
 * @create 2024-05-07 21:58
 * @description:
 */
@Component
@ChannelHandler.Sharable
public class GroupMembersRequestMessageHandler extends MallAbstractChatInboundHandler<GroupMembersRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupMembersRequestMessage msg) throws Exception {
        Set<String> members = groupSessionService.getMembers(msg.getGroupName());
        ctx.writeAndFlush(new GroupMembersResponseMessage(members));
    }
}

