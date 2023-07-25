package com.ww.mall.consumer.template;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.rabbitmq.client.Channel;
import com.ww.mall.enums.MqMsgStatus;
import com.ww.mall.rabbitmq.MqMsgLogEntity;
import com.ww.mall.web.utils.SpringContextManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.io.IOException;
import java.util.Date;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/22 22:32
 **/
@Slf4j
public abstract class MsgConsumerTemplate {

    private static final Integer MSG_TRY_COUNT = 3;

    private final MongoTemplate mongoTemplate = SpringContextManager.getBean(MongoTemplate.class);

    public final void consumer(Message message, Object msg, Channel channel) throws IOException {
        MessageProperties properties = message.getMessageProperties();
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
        Criteria criteria = Criteria.where("msgId").is(correlationId);
        Update update = new Update();
        update.set("updateTime", DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));
        update.set("status", MqMsgStatus.DELIVER_SUCCESS);
        // 消费确认
        channel.basicAck(tag, false);
        log.info("消费【{}】消息完毕", correlationId);
        mongoTemplate.updateFirst(new Query().addCriteria(criteria), update, MqMsgLogEntity.class);
    }

    boolean preMsgConsumer(String correlationId, long tag, Channel channel) throws IOException {
        // 查询消息日志
        MqMsgLogEntity mqMsgLog = getMqMsgById(correlationId);
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
    abstract boolean serverHandler(Object msg);

    void exceptionMsgHandler(String correlationId, long tag, Channel channel, Exception e) throws IOException {
        log.error("执行消费业务异常", e);

        Criteria criteria = Criteria.where("msgId").is(correlationId);
        Update update = new Update();
        update.set("updateTime", DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));

        MqMsgLogEntity mqMsgLog = getMqMsgById(correlationId);
        if (mqMsgLog.getTryCount() > MSG_TRY_COUNT) {
            // 重试三次，如果还未消费成功，则改变状态
            channel.basicNack(tag, false, false);
            update.set("status", MqMsgStatus.DELIVER_FAIL);
        } else {
            channel.basicNack(tag, false, true);
            update.set("tryCount", mqMsgLog.getTryCount() + 1);
        }
        mongoTemplate.updateFirst(new Query().addCriteria(criteria), update, MqMsgLogEntity.class);
    }

    private MqMsgLogEntity getMqMsgById(String correlationId) {
        Criteria criteria = Criteria.where("msgId").is(correlationId);
        return mongoTemplate.findOne(new Query().addCriteria(criteria), MqMsgLogEntity.class);
    }

}
