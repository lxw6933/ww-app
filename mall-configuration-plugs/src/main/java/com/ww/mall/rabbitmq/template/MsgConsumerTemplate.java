package com.ww.mall.rabbitmq.template;

import cn.hutool.extra.spring.SpringUtil;
import com.rabbitmq.client.Channel;
import com.ww.mall.common.constant.Constant;
import com.ww.mall.rabbitmq.enums.MqMsgStatus;
import com.ww.mall.rabbitmq.repository.BaseMqLog;
import com.ww.mall.rabbitmq.repository.MqLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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
        MDC.put(Constant.TRACE_ID, traceId);
        log.info("消费消息【{}】", msg);
        long tag = properties.getDeliveryTag();
        // 获取消息的id
        String correlationId = properties.getCorrelationId();
        // 消费前置处理
        boolean flag = preMsgConsumer(correlationId, tag, channel);
        if (!flag) {
            return;
        }
        try {
            // 核心业务处理
            boolean serverFlag = serverHandler(msg);
            if (serverFlag) {
                // 消息成功消费处理
                successMsgHandler(channel, tag, correlationId);
            }
        } catch (Exception e) {
            // 异常消费处理
            exceptionMsgHandler(correlationId, tag, channel, e);
        }
    }

    void successMsgHandler(Channel channel, long tag, String correlationId) throws IOException {
        // 消费确认
        channel.basicAck(tag, false);
        log.info("【tag：{}】【消息：{}】消费完成", tag, correlationId);
        mqLogRepository.update(correlationId, MqMsgStatus.CONSUMED_SUCCESS);
    }

    boolean preMsgConsumer(String correlationId, long tag, Channel channel) throws IOException {
        // 查询消息日志
        BaseMqLog mqMsgLog = getMqMsgById(correlationId);
        // 消费幂等性, 防止消息被重复消费
        if (mqMsgLog != null && MqMsgStatus.CONSUMED_SUCCESS.equals(mqMsgLog.getStatus())) {
            log.warn("重复消费, correlationId: {}", correlationId);
            // 将消息从队列移除
            channel.basicNack(tag, false, false);
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

    void exceptionMsgHandler(String correlationId, long tag, Channel channel, Exception e) throws IOException {
        log.error("【tag：{}】【消息：{}】消费异常", tag, correlationId, e);
        BaseMqLog mqMsgLog = getMqMsgById(correlationId);
        mqLogRepository.update(correlationId, MqMsgStatus.CONSUMED_FAIL);
        // 重试三次，如果还未消费成功，则改变状态
        channel.basicNack(tag, false, mqMsgLog.getTryCount() <= MSG_TRY_COUNT);
    }

    private BaseMqLog getMqMsgById(String correlationId) {
        return mqLogRepository.get(correlationId);
    }

}
