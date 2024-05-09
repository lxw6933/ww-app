package com.ww.mall.netty.handler;

import com.ww.mall.netty.handler.chat.*;
import com.ww.mall.netty.holder.ClientSocketHolder;
import com.ww.mall.netty.protocol.MallProtocolFrameDecoder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
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
    private ChatQuitHandler chatQuitHandler;
    @Resource
    private PingMessageHandler pingMessageHandler;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ClientSocketHolder.put(ctx.channel().id().asLongText(), (NioSocketChannel) ctx.channel());
        log.info("有新客户端【{}】建立连接, 目前客户端连接数：{}", ctx.channel(), ClientSocketHolder.getAllClientSocket().size());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ClientSocketHolder.removeClientSocket((NioSocketChannel) ctx.channel());
        log.info("客户端断开连接：{}", ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("客户端【{}】出现异常：{}", ctx.channel(), cause.getMessage());
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
                .addLast(new MallProtocolFrameDecoder())
                .addLast(messageCodecHandler)
                .addLast(new IdleStateHandler(30, 0, 0))
                .addLast(pingMessageHandler)
                .addLast(loginRequestMessageHandler)
                .addLast(chatRequestMessageHandler)
                .addLast(groupCreateRequestMessageHandler)
                .addLast(groupJoinRequestMessageHandler)
                .addLast(groupMembersRequestMessageHandler)
                .addLast(groupQuitRequestMessageHandler)
                .addLast(groupChatRequestMessageHandler)
                .addLast(chatQuitHandler);
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
