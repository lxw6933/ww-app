package com.ww.app.im.consumer;

import com.ww.app.common.constant.Constant;
import com.ww.app.common.thread.ThreadMdcUtil;
import com.ww.app.im.api.common.ImBizMqConstant;
import com.ww.app.im.core.api.common.ImMsgBody;
import com.ww.app.im.service.MsgService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
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
public class ImBizConsumer {

    @Resource
    private MsgService msgService;

    @RabbitListener(queues = {ImBizMqConstant.IM_BIZ_MSG_QUEUE}, containerFactory = "appDirectContainerFactory")
    public void imBizMsg(Message message, ImMsgBody imMsgBody) {
        MessageProperties properties = message.getMessageProperties();
        String traceId = properties.getHeader(Constant.TRACE_ID);
        ThreadMdcUtil.setTraceId(traceId);
        log.info("收到客户端发来的业务消息：{}", imMsgBody);
        msgService.handleImMsg(imMsgBody);
    }

}
