package com.ww.app.im.consumer;

import com.ww.app.im.core.api.common.ImMsgBody;
import com.ww.app.im.router.api.common.ImRouterMqConstant;
import com.ww.app.im.service.ImRouterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2025-01-14- 18:05
 * @description:
 *     DirectRabbitListenerContainerFactory：适合高实时性、低延迟的 IM 消息场景（如私聊消息）[不支持批量消费]。
 *     SimpleRabbitListenerContainerFactory：适合高并发、动态扩展的 IM 消息场景（如群聊消息）。
 */
@Slf4j
@Component
public class ImRouterConsumer {

    @Resource
    private ImRouterService imRouterService;

    @RabbitListener(queues = {ImRouterMqConstant.IM_ROUTER_MSG_QUEUE}, containerFactory = "appDirectContainerFactory")
    public void imRouterMsg(Message message, ImMsgBody imMsgBody) {
        log.info("消费消息[{}]", imMsgBody);
        imRouterService.sendMsg(imMsgBody);
    }

}
