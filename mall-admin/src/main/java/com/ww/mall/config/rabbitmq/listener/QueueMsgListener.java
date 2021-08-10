package com.ww.mall.config.rabbitmq.listener;

import com.rabbitmq.client.Channel;
import com.ww.mall.mvc.entity.SysRoleEntity;
import com.ww.mall.mvc.entity.SysUserEntity;
import com.ww.mall.mvc.entity.User;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @description: 消息队列监听器
 * @author: ww
 * @create: 2021-06-28 15:20
 */
@Component
@RabbitListener(queues = {"order-release-queue"})
public class QueueMsgListener {

    /**
     *
     * queue：可以有很多客户端监听
     * 问题1：只要消息被一个监听接收了，队列就会删除消息，同一个消息只能有一个监听器接收到
     * 问题2：接收到消息进行业务处理，处理完才接收队列下一个消息
     *
     * 1: @RabbitListener 标注 Class+Method
     * 2: @RabbitHandler  标注 Method
     * 解决一个queue有多种数据类型
     *
     * @param message 原始消息
     * @param content 消息内容，Springboot会自动转换类型
     */
//    @RabbitListener(queues = {"hello-queue", "ww-queue"})
    @RabbitHandler
    public void receiveMessage(Message message, User content, Channel channel) {
        // 消息头
        MessageProperties header = message.getMessageProperties();
        // 消息体
        byte[] body = message.getBody();
        // 监听队列的消息
        System.out.println("接收到消息："+message+"===类型："+message.getClass());

        System.out.println("消息头："+header);
        System.out.println("消息体："+body);
        System.out.println("消息内容："+content);
        System.out.println("消息通道："+channel);
    }

    @RabbitHandler
    public void receiveMessage(Message message, SysRoleEntity content, Channel channel) {
        System.out.println("SysRoleEntityHandler接收到消息内容："+content);
        // channel内按顺序递增
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        // 手动签收确认消费消息
        try {
            // 处理业务
            // false:不批量处理
            channel.basicAck(deliveryTag, false);
            // false：丢弃消息，不重新入队
//            channel.basicNack(deliveryTag, false, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RabbitHandler
    public void receiveMessage(Message message, SysUserEntity content, Channel channel) {
        System.out.println("SysUserEntityHandler接收到消息内容："+content);
    }

}
