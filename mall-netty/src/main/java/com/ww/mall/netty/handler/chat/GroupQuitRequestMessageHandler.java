package com.ww.mall.netty.handler.chat;

import com.ww.mall.netty.message.chat.req.GroupQuitRequestMessage;
import com.ww.mall.netty.message.chat.res.GroupJoinResponseMessage;
import com.ww.mall.netty.entity.Group;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-05-07 21:58
 * @description:
 */
@Component
@ChannelHandler.Sharable
public class GroupQuitRequestMessageHandler extends MallAbstractChatInboundHandler<GroupQuitRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupQuitRequestMessage msg) throws Exception {
        Group group = groupSessionService.removeMember(msg.getGroupName(), msg.getUsername());
        if (group != null) {
            ctx.writeAndFlush(new GroupJoinResponseMessage(true, "已退出群" + msg.getGroupName()));
        } else {
            ctx.writeAndFlush(new GroupJoinResponseMessage(true, msg.getGroupName() + "群不存在"));
        }
    }
}
