package com.ww.mall.im.service.impl;

import com.ww.mall.im.common.ImMsg;
import com.ww.mall.im.common.ImMsgBody;
import com.ww.mall.im.enums.ImMsgCodeEnum;
import com.ww.mall.im.service.MsgAckService;
import com.ww.mall.im.service.MsgRouterService;
import com.ww.mall.im.utils.ImChannelHandlerContextUtils;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * @author ww
 * @create 2024-12-24 17:39
 * @description:
 */
@Slf4j
@Service
public class MsgRouterServiceImpl implements MsgRouterService {

    @Resource
    private MsgAckService msgAckService;

    @Override
    public void onReceive(ImMsgBody imMsgBody) {
        // 需要进行消息通知的userid
        if (this.sendMsgToClient(imMsgBody)) {
            // 成功发送消息给到客户端，等待客户端接收消息ack
            msgAckService.recordMsgAck(imMsgBody, 1);
            msgAckService.sendDelayMsg(imMsgBody);
        }
    }

    @Override
    public boolean sendMsgToClient(ImMsgBody imMsgBody) {
        // 接收消息的用户id
        Long userId = imMsgBody.getUserId();
        // 获取用户对应的渠道信息
        ChannelHandlerContext ctx = ImChannelHandlerContextUtils.get(userId);
        if (ctx != null) {
            imMsgBody.setSeqId(UUID.randomUUID().toString());
            ImMsg respMsg = ImMsg.build(ImMsgCodeEnum.IM_BIZ_MSG.getCode(), imMsgBody);
            ctx.writeAndFlush(respMsg);
            return true;
        }
        return false;
    }

}
