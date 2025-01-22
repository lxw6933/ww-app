package com.ww.app.rabbitmq.template;

import com.rabbitmq.client.Channel;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.thread.ThreadMdcUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/22 22:32
 **/
@Slf4j
public abstract class MsgConsumerTemplate<T> {

    public final void consumer(Message message, T msg, Channel channel) throws IOException {
        MessageProperties properties = message.getMessageProperties();
        String traceId = properties.getHeader(Constant.TRACE_ID);
        ThreadMdcUtil.setTraceId(traceId);
        log.info("消费消息【{}】", msg);
        // 消费前置处理
        if (!preMsgConsumer(properties, channel)) {
            return;
        }
        try {
            // 核心业务处理
            serverHandler(msg);
            // 消息成功消费处理
            successMsgHandler(properties, channel);
        } catch (Exception e) {
            // 异常消费处理
            exceptionMsgHandler(properties, channel, e);
        }
    }

    protected void successMsgHandler(MessageProperties properties, Channel channel) {
        log.info("【tag：{}】【消息：{}】消费完成", properties.getDeliveryTag(), properties.getCorrelationId());
    }

    protected boolean preMsgConsumer(MessageProperties properties, Channel channel) {
        return true;
    }

    /**
     * 服务核心业务逻辑处理
     *
     * @param msg 消息
     * @return 业务是否成功
     */
    public abstract boolean serverHandler(T msg);

    protected void exceptionMsgHandler(MessageProperties properties, Channel channel, Exception e) {
        log.error("【tag：{}】【消息：{}】消费异常", properties.getDeliveryTag(), properties.getCorrelationId(), e);
        throw new ApiException("消息消费异常");
    }

}
