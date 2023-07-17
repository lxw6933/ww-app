package com.ww.mall.rabbitmq;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/16 00:30
 **/
public interface RabbitmqConsumer {

    /**
     * 消费消息
     *
     * @param message 消息
     * @param t 消息类型
     * @param channel 通道
     * @param springContextManager springBean
     */
//    void consumer(Message message, T t, Channel channel, SpringContextManager springContextManager);

}
