package com.ww.mall.netty.handler.chat;

import com.ww.mall.netty.message.chat.req.GroupJoinRequestMessage;
import com.ww.mall.netty.message.chat.res.GroupJoinResponseMessage;
import com.ww.mall.netty.entity.Group;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-05-07 21:57
 * @description:
 */
@Component
@ChannelHandler.Sharable
public class GroupJoinRequestMessageHandler extends MallAbstractChatInboundHandler<GroupJoinRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupJoinRequestMessage msg) throws Exception {
        Group group = groupSessionService.joinMember(msg.getGroupName(), msg.getUsername());
        if (group != null) {
            ctx.writeAndFlush(new GroupJoinResponseMessage(true, msg.getGroupName() + "群加入成功"));
        } else {
            ctx.writeAndFlush(new GroupJoinResponseMessage(true, msg.getGroupName() + "群不存在"));
        }
    }
}
