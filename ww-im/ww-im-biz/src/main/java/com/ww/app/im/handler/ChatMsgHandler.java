package com.ww.app.im.handler;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.ww.app.disruptor.api.DisruptorTemplate;
import com.ww.app.disruptor.model.Event;
import com.ww.app.im.api.dto.MessageDTO;
import com.ww.app.im.api.enums.ImMsgBizCodeEnum;
import com.ww.app.im.core.api.common.ImMsgBody;
import com.ww.app.im.entity.SingleChatMessage;
import com.ww.app.im.router.api.common.ImRouterMqConstant;
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
    private RabbitMqPublisher rabbitMqPublisher;
    
    @Resource
    private DisruptorTemplate<SingleChatMessage> persistenceDisruptorTemplate;

    @Override
    public void handle(ImMsgBody imMsgBody) {
        MessageDTO messageDTO = JSON.parseObject(imMsgBody.getBizMsg(), MessageDTO.class);
        SingleChatMessage msg = SingleChatMessage.build(imMsgBody.getUserId(), messageDTO);
        
        log.info("接收到[{}]发来的消息:{}", imMsgBody.getUserId(), msg);
        
        // 优化：使用 Disruptor 异步持久化，提升性能
        String eventId = System.currentTimeMillis() + StrUtil.DASHED + msg.getSenderId() + StrUtil.DASHED + msg.getReceiverId();
        Event<SingleChatMessage> persistenceEvent = new Event<>(eventId, "chat-msg", msg);
        
        boolean persistSuccess = persistenceDisruptorTemplate.publish(persistenceEvent);
        if (!persistSuccess) {
            log.error("消息持久化队列已满，消息ID: {}, 用户ID: {}", msg.getId(), imMsgBody.getUserId());
            // TODO: 考虑降级策略
            // 1. 重试机制
            // 2. 存储到Redis备份队列
            // 3. 直接同步存储到MongoDB
            // 当前策略：继续路由转发，但记录告警
        }
        
        // 消息路由转发
        rabbitMqPublisher.sendMsg(ImRouterMqConstant.IM_ROUTER_EXCHANGE, 
                ImRouterMqConstant.IM_ROUTER_MSG_KEY, imMsgBody);
    }

    @Override
    public boolean supports(ImMsgBody imMsgBody) {
        return imMsgBody.getBizCode() == ImMsgBizCodeEnum.CHAT_MSG_BIZ.getCode();
    }
}
