package com.ww.mall.netty.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ww
 * @create 2024-05-06- 16:43
 * @description: netty heart beat handler
 */
@Slf4j
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 判断evt是否是IdleStateEvent（用于触发用户事件，包含 读空闲/写空闲/读写空闲 ）
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                log.info("进入读空闲...");
                ctx.channel().close();
            } else if (event.state() == IdleState.WRITER_IDLE) {
                log.info("进入写空闲...");
                ctx.channel().close();
            } else if (event.state() == IdleState.ALL_IDLE) {
                log.info("关闭无用的Channel，以防资源浪费。Channel Id：{}", ctx.channel().id());
                ctx.channel().close();
            }
        }
    }

}
