package com.ww.mall.netty.handler;

import com.ww.mall.netty.handler.chat.*;
import com.ww.mall.netty.protocol.MallProtocolFrameDecoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * @author ww
 * @create 2024-05-09- 09:19
 * @description:
 */
public class ServerHandlerInitializer extends ChannelInitializer<Channel> {

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ch.pipeline()
                .addLast(new MallProtocolFrameDecoder())
                .addLast(new MessageCodecHandler())
                .addLast(new IdleStateHandler(30, 0, 0))
                .addLast(new PingMessageHandler())
                .addLast(new LoginRequestMessageHandler())
                .addLast(new ChatRequestMessageHandler())
                .addLast(new GroupCreateRequestMessageHandler())
                .addLast(new GroupJoinRequestMessageHandler())
                .addLast(new GroupMembersRequestMessageHandler())
                .addLast(new GroupQuitRequestMessageHandler())
                .addLast(new GroupChatRequestMessageHandler())
                .addLast(new ChatQuitHandler());
//        ch.pipeline()
//                //空闲检测
//                .addLast(new ServerIdleStateHandler())
//                .addLast(new ProtobufVarint32FrameDecoder())
//                .addLast(new ProtobufDecoder(MessageBase.Message.getDefaultInstance()))
//                .addLast(new ProtobufVarint32LengthFieldPrepender())
//                .addLast(new ProtobufEncoder())
//                .addLast(new NettyServerHandler());
    }
}
