package com.ww.app.rabbitmq.config;

import com.ww.app.common.enums.MqMsgStatus;
import com.ww.app.common.utils.json.JacksonUtils;
import com.ww.app.rabbitmq.RabbitMqPublisher;
import com.ww.app.rabbitmq.common.BaseMqLog;
import com.ww.app.rabbitmq.common.MyCorrelationData;
import com.ww.app.rabbitmq.common.RabbitmqConstant;
import com.ww.app.rabbitmq.repository.DefaultMqLogRepository;
import com.ww.app.rabbitmq.repository.MqLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Correlation;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

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
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter,
                                         MqLogRepository<String, ? extends BaseMqLog> mqLogRepository) {
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
                log.warn("收到非规范消息确认回调: {}", correlationData);
                return;
            }
            MyCorrelationData<?> myCorrelationData = (MyCorrelationData<?>) correlationData;
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
            Integer delay = returned.getMessage().getMessageProperties().getHeader(RabbitmqConstant.DELAY_HEADER);
            if (delay > 0) {
                // 解决延时消息会触发的bug
                return;
            }
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
            try {
                correlationData.setMessage(JacksonUtils.parseObject(returned.getMessage().getBody(), Object.class));
            } catch (Exception e) {
                log.warn("消息反序列化失败，使用空消息体记录回执", e);
                correlationData.setMessage(null);
            }
            correlationData.setExchange(returned.getExchange());
            correlationData.setRoutingKey(returned.getRoutingKey());
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
                    messageProperties.setHeader(RabbitmqConstant.EXCHANGE_HEADER, myCorrelationData.getExchange());
                    messageProperties.setHeader(RabbitmqConstant.ROUTING_KEY_HEADER, myCorrelationData.getRoutingKey());
                    messageProperties.setHeader(RabbitmqConstant.DELAY_HEADER, myCorrelationData.getDelayTime());
                    // 延时消息
                    long delayMillis = Math.max(0L, (long) myCorrelationData.getDelayTime() * 1000L);
                    if (delayMillis > Integer.MAX_VALUE) {
                        delayMillis = Integer.MAX_VALUE;
                    }
                    messageProperties.setDelay((int) delayMillis);
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
    @ConditionalOnMissingBean(RabbitMqPublisher.class)
    public RabbitMqPublisher mallPublisher() {
        return new RabbitMqPublisher();
    }

    @Bean
    @ConditionalOnMissingBean(MqLogRepository.class)
    public MqLogRepository<String, ? extends BaseMqLog> mqLogRepository() {
        log.info("初始化消息默认日志持久化");
        return new DefaultMqLogRepository();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory appBatchContainerFactory(ConnectionFactory connectionFactory, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
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

    @Bean
    public DirectRabbitListenerContainerFactory appDirectContainerFactory(ConnectionFactory connectionFactory, MessageConverter converter) {
        DirectRabbitListenerContainerFactory factory = new DirectRabbitListenerContainerFactory();
        // 设置连接工厂
        factory.setConnectionFactory(connectionFactory);
        // 设置每个队列的消费者数量
        factory.setConsumersPerQueue(20);
        // 设置每个消费者的预取消息数量
        factory.setPrefetchCount(10);
        // 设置消息转换器
        factory.setMessageConverter(converter);
        return factory;
    }

}
