package com.ww.app.im.handler;

import com.ww.app.common.utils.IdUtil;
import com.ww.app.im.utils.ImContextUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2025-02-11 10:58
 * @description: traceId handler
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class TraceIdHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 设置traceId
        ImContextUtils.setTraceId(ctx, IdUtil.nextIdStr());
        try {
            // 将消息传递给下一个 Handler
            super.channelRead(ctx, msg);
        } finally {
            // 清理 traceId
            ImContextUtils.removeTraceId(ctx);
        }
    }

}
