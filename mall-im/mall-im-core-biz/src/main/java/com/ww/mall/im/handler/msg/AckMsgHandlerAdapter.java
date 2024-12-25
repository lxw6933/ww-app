package com.ww.mall.im.handler.msg;

import com.alibaba.fastjson.JSON;
import com.ww.mall.im.common.ImMsg;
import com.ww.mall.im.common.ImMsgBody;
import com.ww.mall.im.enums.ImMsgCodeEnum;
import com.ww.mall.im.service.MsgAckService;
import com.ww.mall.im.utils.ImContextUtils;
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

    @Override
    public void handle(ChannelHandlerContext ctx, ImMsg imMsg) {
        Long userId = ImContextUtils.getUserId(ctx);
        Integer appid = ImContextUtils.getAppId(ctx);
        if (userId == null && appid == null) {
            ctx.close();
            throw new IllegalArgumentException("消息ack消息参数异常");
        }
        msgAckService.doMsgAck(JSON.parseObject(imMsg.getBody(), ImMsgBody.class));
    }

    @Override
    public ImMsgCodeEnum getMsgType() {
        return ImMsgCodeEnum.IM_ACK_MSG;
    }
}
