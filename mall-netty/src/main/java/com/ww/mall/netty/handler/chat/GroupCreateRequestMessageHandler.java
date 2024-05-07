package com.ww.mall.netty.handler.chat;

import com.ww.mall.netty.message.chat.req.GroupCreateRequestMessage;
import com.ww.mall.netty.message.chat.res.GroupCreateResponseMessage;
import com.ww.mall.netty.entity.Group;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * @author ww
 * @create 2024-05-07 21:56
 * @description:
 */
@Component
@ChannelHandler.Sharable
public class GroupCreateRequestMessageHandler extends MallAbstractChatInboundHandler<GroupCreateRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupCreateRequestMessage msg) throws Exception {
        String groupName = msg.getGroupName();
        Set<String> members = msg.getMembers();
        // 群管理器
        Group group = groupSessionService.createGroup(groupName, members);
        if (group == null) {
            // 发生成功消息
            ctx.writeAndFlush(new GroupCreateResponseMessage(true, groupName + "创建成功"));
            // 发送拉群消息
            List<Channel> channels = groupSessionService.getMembersChannel(groupName);
            for (Channel channel : channels) {
                channel.writeAndFlush(new GroupCreateResponseMessage(true, "您已被拉入" + groupName));
            }
        } else {
            ctx.writeAndFlush(new GroupCreateResponseMessage(false, groupName + "已经存在"));
        }
    }
}
