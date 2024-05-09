package com.ww.mall.netty.handler;

import com.ww.mall.netty.handler.chat.*;
import com.ww.mall.netty.protocol.MallProtocolFrameDecoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-05-09- 09:19
 * @description:
 */
@Slf4j
@Component
public class ServerHandlerInitializer extends ChannelInitializer<SocketChannel> {

    @Resource
    private MessageCodecHandler messageCodecHandler;
    @Resource
    private LoginRequestMessageHandler loginRequestMessageHandler;
    @Resource
    private ChatRequestMessageHandler chatRequestMessageHandler;
    @Resource
    private GroupCreateRequestMessageHandler groupCreateRequestMessageHandler;
    @Resource
    private GroupJoinRequestMessageHandler groupJoinRequestMessageHandler;
    @Resource
    private GroupMembersRequestMessageHandler groupMembersRequestMessageHandler;
    @Resource
    private GroupQuitRequestMessageHandler groupQuitRequestMessageHandler;
    @Resource
    private GroupChatRequestMessageHandler groupChatRequestMessageHandler;
    @Resource
    private ServerClientHandler serverClientHandler;
    @Resource
    private PingMessageHandler pingMessageHandler;

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
                .addLast(new IdleStateHandler(30, 0, 0))
                .addLast(serverClientHandler)
                .addLast(new MallProtocolFrameDecoder())
                .addLast(messageCodecHandler)
                .addLast(pingMessageHandler)
                .addLast(loginRequestMessageHandler)
                .addLast(chatRequestMessageHandler)
                .addLast(groupCreateRequestMessageHandler)
                .addLast(groupJoinRequestMessageHandler)
                .addLast(groupMembersRequestMessageHandler)
                .addLast(groupQuitRequestMessageHandler)
                .addLast(groupChatRequestMessageHandler);
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
