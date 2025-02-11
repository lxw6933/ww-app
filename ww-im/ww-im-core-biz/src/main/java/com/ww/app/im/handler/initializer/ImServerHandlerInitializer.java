package com.ww.app.im.handler.initializer;

import com.ww.app.im.handler.TraceIdHandler;
import com.ww.app.im.handler.codec.ImMsgCodecHandler;
import com.ww.app.im.handler.server.ImMsgServerHandler;
import com.ww.app.im.protocol.ImProtocolFrameDecoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-11-10 17:14
 * @description: im handler initializer
 */
@Slf4j
@Component
public class ImServerHandlerInitializer extends ChannelInitializer<SocketChannel> {

    @Resource
    private ImMsgServerHandler imMsgServerHandler;

    @Resource
    private TraceIdHandler traceIdHandler;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        // 用来判断是不是读空闲时间过长，或写空闲时间过长 (读，写，读写空闲时间限制) 0表示不关心
//        ch.pipeline().addLast(new IdleStateHandler(imProperties.getDisconnectClientTime(), 0, 0));
                    /*
                    ################################################################
                    #####  ChannelDuplexHandler 可以同时作为 入站 和 出站处理器  #######
                    ##### 12 秒内 没读到数据 触发   IdleState.READER_IDLE       #######
                    #####       写         触发   IdleState.WRITER_IDLE       #######
                    #####     读写         触发   IdleState.ALL_IDLE          #######
                    ################################################################
                     */
//        ch.pipeline().addLast(new ChannelDuplexHandler() {
//            // 【用来处理 读写之外的 特殊的事件】
//            @Override
//            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
//                IdleStateEvent event = (IdleStateEvent) evt;
//                // 是否读超时
//                if (event.state() == IdleState.READER_IDLE) {
//                    log.debug("==============================已经12秒没读到数据了！====================================");
//                    ctx.channel().close();
//                }
//            }
//        });
        // 自定义协议
        ch.pipeline().addLast(new ImProtocolFrameDecoder());
        // 日志
//        ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
        // 自定义协议编解码器
        ch.pipeline().addLast(new ImMsgCodecHandler());
        // traceId 处理器
        ch.pipeline().addLast(traceIdHandler);
        // im 消息处理器
        ch.pipeline().addLast(imMsgServerHandler);
    }
}
