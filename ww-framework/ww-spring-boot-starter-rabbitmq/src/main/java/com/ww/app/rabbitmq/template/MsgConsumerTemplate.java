package com.ww.app.rabbitmq.template;

import cn.hutool.extra.spring.SpringUtil;
import com.rabbitmq.client.Channel;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.enums.MqMsgStatus;
import com.ww.app.common.thread.ThreadMdcUtil;
import com.ww.app.rabbitmq.common.BaseMqLog;
import com.ww.app.rabbitmq.common.MyCorrelationData;
import com.ww.app.rabbitmq.common.RabbitmqConstant;
import com.ww.app.rabbitmq.repository.MqLogRepository;
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

    private static final Integer MSG_TRY_COUNT = 3;

    @SuppressWarnings("unchecked")
    private final MqLogRepository<String, BaseMqLog> mqLogRepository = SpringUtil.getBean(MqLogRepository.class);

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

    void successMsgHandler(MessageProperties properties, Channel channel) throws IOException {
        // 消费确认
        channel.basicAck(properties.getDeliveryTag(), false);
        log.info("【tag：{}】【消息：{}】消费完成", properties.getDeliveryTag(), properties.getCorrelationId());
        MyCorrelationData<Object> correlationData = new MyCorrelationData<>(false);
        correlationData.setTraceId(ThreadMdcUtil.getTraceId());
        correlationData.setId(properties.getCorrelationId());
        mqLogRepository.save(correlationData, MqMsgStatus.CONSUMED_SUCCESS);
    }

    boolean preMsgConsumer(MessageProperties properties, Channel channel) throws IOException {
        // 查询消息日志
        BaseMqLog mqMsgLog = getMqMsgById(properties.getCorrelationId());
        // 消费幂等性, 防止消息被重复消费
        if (mqMsgLog != null && MqMsgStatus.CONSUMED_SUCCESS.equals(mqMsgLog.getStatus())) {
            log.warn("重复消费, correlationId: {}", properties.getCorrelationId());
            // 将消息从队列移除
            channel.basicNack(properties.getDeliveryTag(), false, false);
            return false;
        }
        return true;
    }

    /**
     * 服务核心业务逻辑处理
     *
     * @param msg 消息
     * @return 业务是否成功
     */
    public abstract boolean serverHandler(T msg);

    void exceptionMsgHandler(MessageProperties properties, Channel channel, Exception e) throws IOException {
        log.error("【tag：{}】【消息：{}】消费异常", properties.getDeliveryTag(), properties.getCorrelationId(), e);
        BaseMqLog mqMsgLog = getMqMsgById(properties.getCorrelationId());
        if (mqMsgLog == null) {
            String exchange = properties.getHeader(RabbitmqConstant.EXCHANGE_HEADER);
            String routerKey = properties.getHeader(RabbitmqConstant.ROUTING_KEY_HEADER);
            Object message = properties.getHeader(RabbitmqConstant.MESSAGE_HEADER);

            MyCorrelationData<Object> correlationData = new MyCorrelationData<>(false);
            correlationData.setMessage(message);
            correlationData.setExchange(exchange);
            correlationData.setRoutingKey(routerKey);
            correlationData.setTraceId(ThreadMdcUtil.getTraceId());
            correlationData.setFailCause(e.getMessage());
            correlationData.setId(properties.getCorrelationId());
            mqLogRepository.save(correlationData, MqMsgStatus.CONSUMED_FAIL);
            // 重试三次，如果还未消费成功，则改变状态
            channel.basicNack(properties.getDeliveryTag(), false, true);
        } else {
            mqLogRepository.update(properties.getCorrelationId(), MqMsgStatus.CONSUMED_FAIL);
            // 重试三次，如果还未消费成功，则改变状态
            channel.basicNack(properties.getDeliveryTag(), false, mqMsgLog.getTryCount() <= MSG_TRY_COUNT);
        }
    }

    private BaseMqLog getMqMsgById(String correlationId) {
        return mqLogRepository.get(correlationId);
    }

}
