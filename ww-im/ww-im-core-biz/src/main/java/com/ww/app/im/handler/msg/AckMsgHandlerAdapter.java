package com.ww.app.im.handler.msg;

import com.ww.app.im.common.ImMsg;
import com.ww.app.im.component.ImMsgSerializerComponent;
import com.ww.app.im.enums.ImMsgCodeEnum;
import com.ww.app.im.service.MsgAckService;
import com.ww.app.im.utils.ImContextUtils;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-11-09 20:18
 * @description:
 */
@Slf4j
@Component
public class AckMsgHandlerAdapter implements ImMsgHandlerAdapter {

    @Resource
    private MsgAckService msgAckService;

    @Resource
    private ImMsgSerializerComponent imMsgSerializerComponent;

    @Override
    public void handle(ChannelHandlerContext ctx, ImMsg imMsg) {
        Long userId = ImContextUtils.getUserId(ctx);
        Integer appid = ImContextUtils.getAppId(ctx);
        if (userId == null && appid == null) {
            ctx.close();
            throw new IllegalArgumentException("消息ack消息参数异常");
        }
        msgAckService.doMsgAck(imMsgSerializerComponent.deserializeMsg(imMsg));
    }

    @Override
    public ImMsgCodeEnum getMsgType() {
        return ImMsgCodeEnum.IM_ACK_MSG;
    }
}
