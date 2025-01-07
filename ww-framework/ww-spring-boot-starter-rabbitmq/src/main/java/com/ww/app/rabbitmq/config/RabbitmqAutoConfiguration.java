package com.ww.app.rabbitmq.config;

import cn.hutool.extra.spring.SpringUtil;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.enums.MqMsgStatus;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.thread.ThreadMdcUtil;
import com.ww.app.rabbitmq.RabbitMqPublisher;
import com.ww.app.rabbitmq.common.BaseMqLog;
import com.ww.app.rabbitmq.common.MyCorrelationData;
import com.ww.app.rabbitmq.common.RabbitmqConstant;
import com.ww.app.rabbitmq.repository.DefaultMqLogRepository;
import com.ww.app.rabbitmq.repository.MongoMqLogRepository;
import com.ww.app.rabbitmq.repository.MqLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Correlation;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;

import javax.annotation.PostConstruct;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/15 22:12
 **/
@Slf4j
@EnableRabbit
@ConditionalOnClass({RabbitTemplate.class})
@EnableConfigurationProperties(RabbitProperties.class)
public class RabbitmqAutoConfiguration {

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
    public MessageConverter messageConverter() {
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
            if (!(correlationData instanceof MyCorrelationData)) {
                throw new ApiException("发送失败，请按照规范发送消息");
            }
            MyCorrelationData<?> myCorrelationData = (MyCorrelationData<?>) correlationData;
            ThreadMdcUtil.setTraceId(myCorrelationData.getTraceId());
            if (!ack) {
                log.error("消息发送到Exchange失败, {}, cause: {}", correlationData, cause);
                myCorrelationData.setFailCause(cause);
                mqLogRepository.save(myCorrelationData, MqMsgStatus.DELIVER_FAIL);
            }
        });
        // 是否到达队列回调
        rabbitTemplate.setMandatory(true);
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
            String traceId = returned.getMessage().getMessageProperties().getHeader(Constant.TRACE_ID);
            Integer delay = returned.getMessage().getMessageProperties().getHeader(RabbitmqConstant.DELAY_HEADER);
            if (delay > 0) {
                // 解决延时消息会触发的bug
                return;
            }
            ThreadMdcUtil.setTraceId(traceId);
            // 消息到达queue失败
            log.error("消息发送到Queue失败\n" +
                            "[消息]  >>>  {}\n" +
                            "[replyCode]  >>>  {}\n" +
                            "[replyText]  >>>  {}\n" +
                            "[交换机]  >>>  {}\n" +
                            "[路由key]  >>>  {}\n",
                    new String(returned.getMessage().getBody()),
                    returned.getReplyCode(),
                    returned.getReplyText(),
                    returned.getExchange(),
                    returned.getRoutingKey());

            MyCorrelationData<Object> correlationData = new MyCorrelationData<>(false);
            correlationData.setMessage(new String(returned.getMessage().getBody()));
            correlationData.setExchange(returned.getExchange());
            correlationData.setRoutingKey(returned.getRoutingKey());
            correlationData.setTraceId(traceId);
            correlationData.setFailCause(returned.getReplyText());
            mqLogRepository.save(correlationData, MqMsgStatus.DELIVER_FAIL);
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
                if (correlation instanceof MyCorrelationData) {
                    MyCorrelationData<?> myCorrelationData = (MyCorrelationData<?>) correlation;
                    messageProperties.setHeader(Constant.TRACE_ID, myCorrelationData.getTraceId());
                    messageProperties.setHeader(RabbitmqConstant.EXCHANGE_HEADER, myCorrelationData.getExchange());
                    messageProperties.setHeader(RabbitmqConstant.ROUTING_KEY_HEADER, myCorrelationData.getRoutingKey());
                    messageProperties.setHeader(RabbitmqConstant.MESSAGE_HEADER, myCorrelationData.getMessage());
                    messageProperties.setHeader(RabbitmqConstant.DELAY_HEADER, myCorrelationData.getDelayTime());
                    // 延时消息
                    messageProperties.setDelay(myCorrelationData.getDelayTime() * 1000);
                }
                return this.postProcessMessage(message);
            }

            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                return message;
            }
        };
    }

    @Bean
    public RabbitMqPublisher mallPublisher() {
        return new RabbitMqPublisher();
    }

    @Bean
    @ConditionalOnMissingBean({MqLogRepository.class, MongoMqLogRepository.class})
    public MqLogRepository<String, ? extends BaseMqLog> mqLogRepository() {
        log.info("初始化消息默认日志持久化");
        return new DefaultMqLogRepository();
    }

    @Bean
    @ConditionalOnBean({MongoTemplate.class})
    public MqLogRepository<String, ? extends BaseMqLog> mqLogMongoRepository() {
        log.info("初始化消息日志mongo持久化");
        return new MongoMqLogRepository();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory batchContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        // 启用批量监听
        factory.setBatchListener(true);
        // 设置批量大小
        factory.setBatchSize(100);
        // 设置超时时间（毫秒）
        factory.setReceiveTimeout(5000L);
        // 启用消费者批量模式
        factory.setConsumerBatchEnabled(true);
        // 最小消费者线程数
        factory.setConcurrentConsumers(10);
        // 最大并发消费者线程数
        factory.setMaxConcurrentConsumers(20);
        return factory;
    }

}
