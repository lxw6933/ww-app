package com.ww.mall.config.rabbitmq.consumer.proxy;

import com.rabbitmq.client.Channel;
import com.ww.mall.mvc.manager.SpringContextManager;
import org.springframework.amqp.core.Message;

/**
 * @description: 消费者接口（jdk代理）
 * @author: ww
 * @create: 2021/6/29 下午11:01
 **/
public interface BaseConsumer<T> {

    /**
     * 消费消息
     *
     * @param message 消息
     * @param t 消息类型
     * @param channel 通道
     * @param springContextManager springBean
     */
    void consumer(Message message, T t, Channel channel, SpringContextManager springContextManager);

}
