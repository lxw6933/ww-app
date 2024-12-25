package com.ww.mall.im.handler.msg;

import com.ww.mall.im.common.ImMsg;
import com.ww.mall.im.enums.ImMsgCodeEnum;
import com.ww.mall.im.utils.ImContextUtils;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-11-09 20:18
 * @description:
 */
@Slf4j
@Component
public class BizMsgHandlerAdapter implements ImMsgHandlerAdapter {

    @Override
    public void handle(ChannelHandlerContext ctx, ImMsg imMsg) {
        Long userId = ImContextUtils.getUserId(ctx);
        Integer appId = ImContextUtils.getAppId(ctx);
        if (userId == null || appId == null) {
            log.error("attr error,imMsg is {}", imMsg);
            ctx.close();
            throw new IllegalArgumentException("attr is error");
        }
        byte[] body = imMsg.getBody();
        if (body == null || body.length == 0) {
            log.error("body error,imMsg is {}", imMsg);
            return;
        }
        // TODO 发送mq消费处理消息推送

    }

    @Override
    public ImMsgCodeEnum getMsgType() {
        return ImMsgCodeEnum.IM_BIZ_MSG;
    }
}
