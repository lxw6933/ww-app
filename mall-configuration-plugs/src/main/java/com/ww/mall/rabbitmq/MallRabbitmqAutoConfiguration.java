package com.ww.mall.rabbitmq;

import cn.hutool.extra.spring.SpringUtil;
import com.ww.mall.common.constant.Constant;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.annotation.enable.EnableMallMongodb;
import com.ww.mall.mongodb.repository.MongoMqLogRepository;
import com.ww.mall.mongodb.repository.MqMsgLogEntity;
import com.ww.mall.rabbitmq.enums.MqMsgStatus;
import com.ww.mall.rabbitmq.repository.BaseMqLog;
import com.ww.mall.rabbitmq.repository.MqLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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

import javax.annotation.PostConstruct;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/15 22:12
 **/
@Slf4j
@EnableRabbit
@EnableMallMongodb
@ConditionalOnClass({RabbitTemplate.class})
@EnableConfigurationProperties(RabbitProperties.class)
public class MallRabbitmqAutoConfiguration {

    private MqLogRepository<String, BaseMqLog> mqLogRepository;

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() {
        mqLogRepository = SpringUtil.getBean(MqLogRepository.class);
        log.info("消息日志持久化处理器初始化完成：{}", mqLogRepository);
    }

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
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        // 设置对象消息转换器
        rabbitTemplate.setMessageConverter(converter);
        /**
         * 设置消息发送到Broker确认回调
         *
         * @param correlationData 当前消息的唯一关联数据（消息唯一id）
         * @param ack 消息是否成功被broker收到
         * @param cause 失败的原因
         */
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!(correlationData instanceof MallCorrelationData)) {
                throw new ApiException("发送失败，请按照规范发送消息");
            }
            MallCorrelationData<?> mallCorrelationData = (MallCorrelationData<?>) correlationData;
            MDC.put(Constant.TRACE_ID, mallCorrelationData.getTraceId());
            if (!ack) {
                log.error("消息发送到Exchange失败, {}, cause: {}", correlationData, cause);
                if (((MallCorrelationData<?>) correlationData).isMsgMode()) {
                    mqLogRepository.update(mallCorrelationData.getId(), MqMsgStatus.DELIVER_FAIL);
                }
            }
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
            if (returned.getMessage().getMessageProperties().getHeader(Constant.MALL_MSG_MODE)) {
                mqLogRepository.update(returned.getMessage().getMessageProperties().getCorrelationId(), MqMsgStatus.DELIVER_FAIL);
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
                if (correlation instanceof MallCorrelationData) {
                    String traceId = ((MallCorrelationData<?>) correlation).getTraceId();
                    boolean msgMode = ((MallCorrelationData<?>) correlation).isMsgMode();
                    messageProperties.setHeader(Constant.TRACE_ID, traceId);
                    messageProperties.setHeader(Constant.MALL_MSG_MODE, msgMode);
                }
                return message;
            }

            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                return message;
            }
        };
    }

    @Bean
    public MallPublisher mallPublisher() {
        return new MallPublisher();
    }

    @Bean
    public MqLogRepository<String, MqMsgLogEntity> mongoMqLogRepository() {
        log.info("初始化消息日志mongo持久化");
        return new MongoMqLogRepository();
    }

}
