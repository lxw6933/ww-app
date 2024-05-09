package com.ww.mall.netty.handler;

import com.ww.mall.netty.handler.chat.HeartbeatAckHandler;
import com.ww.mall.netty.handler.chat.ClientHandler;
import com.ww.mall.netty.handler.chat.MessageCodecHandler;
import com.ww.mall.netty.protocol.MallProtocolFrameDecoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-05-09- 09:28
 * @description:
 */
@Component
public class ClientHandlerInitializer extends ChannelInitializer<SocketChannel> {

    @Resource
    private PongMessageHandler pongMessageHandler;

    @Resource
    private MessageCodecHandler messageCodecHandler;

    @Resource
    private ClientHandler clientHandler;

    @Resource
    private HeartbeatAckHandler heartbeatAckHandler;

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
                .addLast(new IdleStateHandler(0, 5, 0))
                .addLast(clientHandler)
                .addLast(new MallProtocolFrameDecoder())
                .addLast(messageCodecHandler)
                .addLast(pongMessageHandler)
                .addLast(heartbeatAckHandler);
//        ch.pipeline()
//                .addLast(new IdleStateHandler(0, 10, 0))
//                .addLast(new ProtobufVarint32FrameDecoder())
//                .addLast(new ProtobufDecoder(MessageBase.Message.getDefaultInstance()))
//                .addLast(new ProtobufVarint32LengthFieldPrepender())
//                .addLast(new ProtobufEncoder())
//                .addLast(new HeartBeatHandler())
//                .addLast(new NettyClientHandler());
    }
}
