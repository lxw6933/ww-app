package com.ww.mall.config.rabbitmq;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ww.mall.common.constant.Constant;
import com.ww.mall.common.utils.JsonUtils;
import com.ww.mall.mvc.entity.MqMsgLogEntity;
import com.ww.mall.mvc.service.MqMsgLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Correlation;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @description: rabbitmq消息队列核心配置 (RabbitAutoConfiguration 自动配置)
 * 注：@EnableRabbit 监听队列必须要开启此注解 @RabbitListener 才有效
 * @author: ww
 * @create: 2021-06-28 14:15
 */
@Slf4j
@EnableRabbit
@Configuration
public class RabbitmqConfig {

    @Resource
    private MqMsgLogService mqMsgLogService;

    @Resource
    private CachingConnectionFactory connectionFactory;

    /**
     * 自定义Jackson消息转换器 用于对象消息的转换
     *
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
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        // 设置对象消息转换器
        rabbitTemplate.setMessageConverter(converter());
        /**
         * 设置消息发送到Broker确认回调
         *
         * @param correlationData 当前消息的唯一关联数据（消息唯一id）
         * @param ack 消息是否成功被broker收到
         * @param cause 失败的原因
         */
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            MyCorrelationData myCorrelationData;
            MqMsgLogEntity mqLog = new MqMsgLogEntity();
            if (correlationData instanceof MyCorrelationData) {
                myCorrelationData = (MyCorrelationData) correlationData;
                mqLog.setRoutingKey(myCorrelationData.getRoutingKey());
                mqLog.setExchange(myCorrelationData.getExchange());
                mqLog.setMessage(JsonUtils.toJson(myCorrelationData.getMessage()));
                mqLog.setMsgId(myCorrelationData.getId());
            }
            if (ack) {
                // 发送成功保存消息日志 状态
                mqLog.setStatus(Constant.MsgLogStatus.DELIVERING);
            } else {
                log.error("消息发送到Exchange失败, {}, cause: {}", correlationData, cause);
                mqLog.setStatus(Constant.MsgLogStatus.DELIVER_FAIL);
            }
            mqMsgLogService.save(mqLog);
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
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            // 消息到达queue失败
            log.error("\n" +
                            "发送信息  >>>  {}\n" +
                            "[replyCode]  >>>  {}\n" +
                            "replyText]  >>>  {}\n" +
                            "交换机  >>>  {}\n" +
                            "路由key  >>>  {}\n",
                    message,
                    replyCode,
                    replyText,
                    exchange,
                    routingKey);
            mqMsgLogService.update(new UpdateWrapper<MqMsgLogEntity>()
                    .eq("msg_id", message.getMessageProperties().getCorrelationId())
                    .set("status", Constant.MsgLogStatus.DELIVER_FAIL)
                    .set("update_time", new Date())
            );
        });
        return rabbitTemplate;
    }


    /**
     * 设置correlationId
     *
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
