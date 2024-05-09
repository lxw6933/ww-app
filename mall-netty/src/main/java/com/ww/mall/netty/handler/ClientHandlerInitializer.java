package com.ww.mall.netty.handler;

import com.ww.mall.netty.handler.chat.MessageCodecHandler;
import com.ww.mall.netty.protocol.MallProtocolFrameDecoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * @author ww
 * @create 2024-05-09- 09:28
 * @description:
 */
public class ClientHandlerInitializer extends ChannelInitializer<Channel> {

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ch.pipeline()
                .addLast(new MallProtocolFrameDecoder())
                .addLast(new MessageCodecHandler())
                .addLast(new IdleStateHandler(0, 10, 0))
                .addLast(new PongMessageHandler());
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
