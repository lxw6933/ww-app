package com.ww.mall.rabbitmq;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.mongodb.client.result.UpdateResult;
import com.ww.mall.common.constant.Constant;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.mongodb.EnableMallMongodb;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Correlation;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/15 22:12
 **/
@Slf4j
@EnableRabbit
@EnableMallMongodb
@ConditionalOnClass({RabbitTemplate.class, MongoTemplate.class})
@EnableConfigurationProperties(RabbitProperties.class)
public class MallRabbitmqAutoConfiguration {

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * 自定义Jackson消息转换器 用于对象消息的转换
     * @return MessageConverter
     */
    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 自定义 RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, RabbitProperties rabbitProperties) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        // 设置对象消息转换器
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        /**
         * 设置消息发送到Broker确认回调
         *
         * @param correlationData 当前消息的唯一关联数据（消息唯一id）
         * @param ack 消息是否成功被broker收到
         * @param cause 失败的原因
         */
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            MallCorrelationData mallCorrelationData;
            MqMsgLogEntity mqLog = new MqMsgLogEntity();
            if (correlationData instanceof MallCorrelationData) {
                mallCorrelationData = (MallCorrelationData) correlationData;
                mqLog.setRoutingKey(mallCorrelationData.getRoutingKey());
                mqLog.setExchange(mallCorrelationData.getExchange());
                mqLog.setMessage(JSON.toJSONString(mallCorrelationData.getMessage()));
                mqLog.setMsgId(mallCorrelationData.getId());
                mqLog.setCreateTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));
                mqLog.setUpdateTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));
            }
            if (ack) {
                // 发送成功保存消息日志 状态
                mqLog.setStatus(Constant.MsgLogStatus.DELIVERING);
            } else {
                log.error("消息发送到Exchange失败, {}, cause: {}", correlationData, cause);
                mqLog.setStatus(Constant.MsgLogStatus.DELIVER_FAIL);
            }
            mongoTemplate.save(mqLog);
        });

        /**
         * 只要消息没有投递到指定的queue，就会触发回调，成功投递不会触发
         *
         * @param message   投递失败的消息内容
         * @param replyCode 回复的状态码
         * @param replyText 回复的文本内容
         * @param exchange  哪个交换机发送的
         * @param routingKey 交换机通过哪个路由键发送到queue
         */
        rabbitTemplate.setReturnsCallback(returned -> {
            // 消息到达queue失败
            log.error("\n" +
                            "发送信息  >>>  {}\n" +
                            "[replyCode]  >>>  {}\n" +
                            "replyText]  >>>  {}\n" +
                            "交换机  >>>  {}\n" +
                            "路由key  >>>  {}\n",
                    returned.getMessage(),
                    returned.getReplyCode(),
                    returned.getReplyText(),
                    returned.getExchange(),
                    returned.getRoutingKey());
            Criteria criteria = Criteria.where("msgId")
                    .is(returned.getMessage().getMessageProperties().getCorrelationId());
            Update update = new Update();
            update.set("status", Constant.MsgLogStatus.DELIVER_FAIL);
            update.set("updateTime", DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));
            UpdateResult updateResult = mongoTemplate.updateFirst(new Query().addCriteria(criteria), update, MqMsgLogEntity.class);
            if (updateResult.getModifiedCount() == 0) {
                log.error("队列日志异常:{}", JSON.toJSONString(returned));
                throw new ApiException("队列日志异常");
            }
        });
        return rabbitTemplate;
    }

    /**
     * 设置correlationId
     * @return MessagePostProcessor
     */
    @Bean
    public MessagePostProcessor messagePostProcessor() {
        return new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message, Correlation correlation) {
                MessageProperties messageProperties = message.getMessageProperties();
                if (correlation instanceof CorrelationData) {
                    String correlationId = ((CorrelationData) correlation).getId();
                    messageProperties.setCorrelationId(correlationId);
                }
                return message;
            }

            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                return message;
            }
        };
    }

}
