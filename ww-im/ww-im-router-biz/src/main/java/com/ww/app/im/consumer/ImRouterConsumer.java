package com.ww.app.im.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.thread.ThreadMdcUtil;
import com.ww.app.im.common.ImMsgBody;
import com.ww.app.im.router.api.common.ImRouterMqConstant;
import com.ww.app.im.service.ImRouterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ww
 * @create 2025-01-14- 18:05
 * @description:
 */
@Slf4j
@Component
public class ImRouterConsumer {

    private final ObjectMapper mapper = new ObjectMapper();

    @Resource
    private ImRouterService imRouterService;

    @RabbitListener(queues = {ImRouterMqConstant.IM_ROUTER_MSG_QUEUE}, containerFactory = "batchContainerFactory")
    public void imRouterMsg(List<Message> msgList) throws IOException {
        List<ImMsgBody> imMsgBodyList = new ArrayList<>();
        for (Message message : msgList) {
            MessageProperties properties = message.getMessageProperties();
            String traceId = properties.getHeader(Constant.TRACE_ID);
            ThreadMdcUtil.setTraceId(traceId);
            ImMsgBody msg = mapper.readValue(message.getBody(), ImMsgBody.class);
            log.info("消费消息[{}]", msg);
            imMsgBodyList.add(msg);
        }
        imRouterService.batchSendMsg(imMsgBodyList);
    }

}
