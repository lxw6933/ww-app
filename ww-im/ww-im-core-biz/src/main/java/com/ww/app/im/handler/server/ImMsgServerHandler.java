package com.ww.app.im.handler.server;

import com.ww.app.disruptor.api.DisruptorTemplate;
import com.ww.app.disruptor.model.Event;
import com.ww.app.im.common.ImMsg;
import com.ww.app.im.event.ImMsgEvent;
import com.ww.app.im.handler.msg.LogoutMsgHandlerAdapter;
import com.ww.app.im.utils.ImContextUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2024-11-10 16:25
 * @description: im msg handler
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class ImMsgServerHandler extends SimpleChannelInboundHandler<Object> {

    @Resource
    private DisruptorTemplate<ImMsgEvent> imMsgDisruptorTemplate;

    @Resource
    private LogoutMsgHandlerAdapter logoutMsgHandlerAdapter;

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ImMsg) {
            ImMsg imMsg = (ImMsg) msg;
            
            // 创建事件并发布到 Disruptor
            ImMsgEvent eventData = new ImMsgEvent();
            eventData.setCtx(ctx);
            eventData.setImMsg(imMsg);
            eventData.setReceiveTime(System.currentTimeMillis());
            
            Event<ImMsgEvent> event = new Event<>(generateEventId(imMsg), "im-msg", eventData);
            
            // 尝试发布，超时1秒
            boolean success = imMsgDisruptorTemplate.tryPublish(event, 1, TimeUnit.SECONDS);
            
            if (!success) {
                log.error("Disruptor队列已满，消息被拒绝: msgType={}, userId={}, channel={}", 
                        imMsg.getMsgType(), ImContextUtils.getUserId(ctx), ctx.channel());
                // 可以考虑：1) 返回错误响应 2) 降级到同步处理 3) 直接关闭连接
                // 这里选择关闭连接，防止客户端继续发送消息
                ctx.close();
            }
        } else {
            log.error("未知消息类型: {}, channel={}", msg.getClass(), ctx.channel());
            ctx.close();
        }
    }
    
    /**
     * 生成事件ID
     */
    private String generateEventId(ImMsg imMsg) {
        return imMsg.getMsgType() + "-" + System.nanoTime();
    }
    
    /**
     * 正常、异常断线都会触发
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("客户端断开连接: {}", ctx.channel());
        Long userId = ImContextUtils.getUserId(ctx);
        Integer appId = ImContextUtils.getAppId(ctx);
        if (userId != null && appId != null) {
            logoutMsgHandlerAdapter.logoutHandler(ctx, userId, appId);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("客户端建立连接: {}", ctx.channel());
    }
}
