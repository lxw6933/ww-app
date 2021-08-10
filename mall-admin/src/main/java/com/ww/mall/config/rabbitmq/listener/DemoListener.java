package com.ww.mall.config.rabbitmq.listener;

import com.rabbitmq.client.Channel;
import com.ww.mall.config.rabbitmq.binding.RabbitMqConstant;
import com.ww.mall.config.rabbitmq.consumer.DemoConsumer;
import com.ww.mall.config.rabbitmq.consumer.proxy.BaseConsumer;
import com.ww.mall.config.rabbitmq.consumer.proxy.BaseConsumerProxy;
import com.ww.mall.mvc.entity.User;
import com.ww.mall.mvc.manager.SpringContextManager;
import com.ww.mall.mvc.service.MqMsgLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;

/**
 * @description:
 * @author: ww
 * @create: 2021/6/30 上午8:38
 **/
@Slf4j
@Component
@RabbitListener(queues = {RabbitMqConstant.DEMO_QUEUE_NAME})
public class DemoListener {

    @Resource
    private MqMsgLogService mqMsgLogService;

    @Resource
    private SpringContextManager springContextManager;

    @Resource
    private TransactionTemplate transactionTemplate;

    @RabbitHandler
    public void receiveMessage(Message message, User content, Channel channel) {
        log.info("监听到MQ demo.queue有新消息:"+ content);
        BaseConsumerProxy proxy = new BaseConsumerProxy(new DemoConsumer(), content.getClass(), mqMsgLogService);
        BaseConsumer<User> instance = (BaseConsumer) proxy.getProxy();

        if (instance != null) {
            //执行消费邮箱消息业务，将
            // 1：消息幂等性
            // 2：消息消费确认性 交给代理类执行，真实消费对象只执行业务
            instance.consumer(message, content, channel, springContextManager);
        }
    }

}
