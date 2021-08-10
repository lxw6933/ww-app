package com.ww.mall.config.rabbitmq.binding.exchange;

import com.ww.mall.config.rabbitmq.binding.RabbitMqConstant;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description: 交换机配置（一个服务对应一个交换机，命名：服务名-业务-exchange）
 * @author: ww
 * @create: 2021/6/29 下午8:44
 **/
@Configuration
public class ExchangeConfig {

    @Bean(name = "demoExchange")
    public TopicExchange demoExchange() {
        return new TopicExchange(RabbitMqConstant.DEMO_EXCHANGE_NAME);
    }

    @Bean(name = "mailExchange")
    public TopicExchange mailExchange() {
        return new TopicExchange(RabbitMqConstant.MAIL_EXCHANGE_NAME);
    }

    @Bean(name = "orderEventExchange")
    public TopicExchange orderEventExchange() {
        return new TopicExchange(RabbitMqConstant.ORDER_EVENT_EXCHANGE);
    }

}
