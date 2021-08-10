package com.ww.mall.config.rabbitmq.consumer;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.rabbitmq.client.Channel;
import com.ww.mall.common.constant.Constant;
import com.ww.mall.config.rabbitmq.consumer.proxy.BaseConsumer;
import com.ww.mall.mvc.entity.MqMsgLogEntity;
import com.ww.mall.mvc.entity.User;
import com.ww.mall.mvc.manager.SpringContextManager;
import com.ww.mall.mvc.service.MqMsgLogService;
import com.ww.mall.mvc.service.UserService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;

/**
 * @description: demo消费者
 * @author: ww
 * @create: 2021/6/30 上午8:43
 **/
public class DemoConsumer implements BaseConsumer<User> {

    @Override
    public void consumer(Message message, User user, Channel channel, SpringContextManager springContextManager) {
        UserService userService = springContextManager.getBean(UserService.class);
        MqMsgLogService mqMsgLogService = springContextManager.getBean(MqMsgLogService.class);
        TransactionTemplate transactionTemplate = springContextManager.getBean(TransactionTemplate.class);
        MessageProperties properties = message.getMessageProperties();
        String correlationId = properties.getCorrelationId();
        transactionTemplate.execute(status -> {
            boolean update = mqMsgLogService.update(new UpdateWrapper<MqMsgLogEntity>()
                    .eq("msg_id", correlationId)
                    .set("status", Constant.MsgLogStatus.CONSUMED_SUCCESS)
                    .set("update_time", new Date())
            );
            // 处理业务逻辑(放入缓存或者落库)
            boolean save = userService.save(user);
            return update && save;
        });
    }

}
