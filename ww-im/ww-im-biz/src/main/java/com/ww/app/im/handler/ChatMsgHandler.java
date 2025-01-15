package com.ww.app.im.handler;

import com.alibaba.fastjson.JSON;
import com.ww.app.im.core.api.common.ImMsgBody;
import com.ww.app.im.api.dto.MessageDTO;
import com.ww.app.im.api.enums.ImMsgBizCodeEnum;
import com.ww.app.im.router.api.common.ImRouterMqConstant;
import com.ww.app.im.router.api.rpc.ImRouterApi;
import com.ww.app.rabbitmq.RabbitMqPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-12-25 21:30
 * @description: 聊天消息处理器
 */
@Slf4j
@Component
public class ChatMsgHandler implements MsgHandler {

    @Resource
    private ImRouterApi imRouterApi;

    @Resource
    private RabbitMqPublisher rabbitMqPublisher;

    @Override
    public void handle(ImMsgBody imMsgBody) {
        log.info("接收到[{}]发来的消息:{}", imMsgBody.getUserId(), JSON.parseObject(imMsgBody.getBizMsg(), MessageDTO.class).getContent());
        // 消息处理、发送消息队列转发消费
        rabbitMqPublisher.sendMsg(ImRouterMqConstant.IM_ROUTER_EXCHANGE, ImRouterMqConstant.IM_ROUTER_MSG_KEY, imMsgBody);
        // 临时使用远程调用来转发
//        imRouterApi.routeMsg(imMsgBody);
    }

    @Override
    public boolean supports(ImMsgBody imMsgBody) {
        return imMsgBody.getBizCode() == ImMsgBizCodeEnum.CHAT_MSG_BIZ.getCode();
    }
}
