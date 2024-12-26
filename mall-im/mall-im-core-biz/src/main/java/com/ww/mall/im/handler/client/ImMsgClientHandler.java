package com.ww.mall.im.handler.client;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson.JSON;
import com.ww.mall.im.common.ImMsg;
import com.ww.mall.im.common.ImMsgBody;
import com.ww.mall.im.component.ImMsgSerializerComponent;
import com.ww.mall.im.enums.ImMsgCodeEnum;
import com.ww.mall.im.starter.ImClientStarter;
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
public class ImMsgClientHandler extends SimpleChannelInboundHandler<ImMsg> {

    @Resource
    private ImMsgSerializerComponent imMsgSerializerComponent;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ImMsg imMsg) {
        ImMsgBody respBody = imMsgSerializerComponent.deserializeMsg(imMsg);
        System.out.println("收到服务端发送的消息" + respBody);
        if (imMsg.getMsgType() == ImMsgCodeEnum.IM_BIZ_MSG.getCode()) {
            ImMsgBody ackBody = new ImMsgBody();
            ackBody.setSeqId(respBody.getSeqId());
            ackBody.setAppId(respBody.getAppId());
            ackBody.setUserId(respBody.getUserId());
            // 通知服务端 客户端确认收到业务消息
            ImMsg ackMsg = ImMsg.build(ImMsgCodeEnum.IM_ACK_MSG.getCode(), JSON.toJSONString(ackBody));
            ctx.writeAndFlush(ackMsg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.warn("im server 异常，三秒后重连");
        ImClientStarter imClientStarter = SpringUtil.getBean(ImClientStarter.class);
        ctx.channel().eventLoop().schedule(imClientStarter::start, 3, TimeUnit.SECONDS);
    }

}
