package com.ww.mall.rabbitmq.queue;

import com.ww.mall.rabbitmq.MallRabbitmqAutoConfiguration;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/21 22:54
 **/
@Configuration
@ConditionalOnBean(MallRabbitmqAutoConfiguration.class)
public class QueueConfiguration {

    @Bean
    public Queue mallMemberRegisterQueue() {
        return new Queue(QueueConstant.MALL_MEMBER_REGISTER_QUEUE_NAME);
    }

    @Bean
    public Queue mallCanalQueue() {
        return new Queue(QueueConstant.MALL_CANAL_QUEUE_NAME);
    }

}
