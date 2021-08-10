package com.ww.mall.config.rabbitmq.consumer.proxy;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.rabbitmq.client.Channel;
import com.ww.mall.common.constant.Constant;
import com.ww.mall.mvc.entity.MqMsgLogEntity;
import com.ww.mall.mvc.service.MqMsgLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.lang.reflect.Proxy;
import java.util.Date;

/**
 * @description: 消费者代理类（使得消费者专注业务的处理，代理者负责消息的处理）
 * @author: ww
 * @create: 2021/6/29 下午11:03
 **/
@Slf4j
public class BaseConsumerProxy {

    private final Object target;
    private MqMsgLogService mqMsgLogService;

    private final Class<?> messageType;

    public BaseConsumerProxy(Object target, Class<?> messageType) {
        this.target = target;
        this.messageType = messageType;
    }

    public BaseConsumerProxy(Object target, Class<?> messageType, MqMsgLogService mqMsgLogService) {
        this.target = target;
        this.messageType = messageType;
        this.mqMsgLogService = mqMsgLogService;
    }

    public Object getProxy() {
        ClassLoader classLoader = target.getClass().getClassLoader();
        Class<?>[] interfaces = target.getClass().getInterfaces();
        return Proxy.newProxyInstance(classLoader, interfaces, (proxy1, method, args) -> {
            //获取代理消费对象的方法参数
            Message message = (Message) args[0];
            Channel channel = (Channel) args[2];

            MessageProperties properties = message.getMessageProperties();
            long tag = properties.getDeliveryTag();

            // 获取消息的id
            String correlationId = properties.getCorrelationId();
            // 消费幂等性, 防止消息被重复消费
            if (isConsumed(correlationId)) {
                log.warn("重复消费, correlationId: {}", correlationId);
                // 将消息从队列移除
                channel.basicNack(tag, false, false);
                return null;
            }

            try {
                // 真正消费的业务逻辑
                Object result = method.invoke(target, args);
                // 消费确认
                channel.basicAck(tag, false);
                log.info("消费【"+correlationId+"】消息完毕：");
                return result;
            } catch (Exception e) {
                log.error("执行消费业务异常！！！", e);
                MqMsgLogEntity mqMsg = mqMsgLogService.getOne(new QueryWrapper<MqMsgLogEntity>()
                        .eq("msg_id", correlationId)
                );
                if (mqMsg.getTryCount() > 3) {
                    // 重试三次，如果还未消费成功，则改变状态
                    channel.basicNack(tag, false, false);
                    mqMsgLogService.update(new UpdateWrapper<MqMsgLogEntity>()
                            .eq("msg_id", correlationId)
                            .set("status", Constant.MsgLogStatus.CONSUMED_FAIL)
                            .set("update_time", new Date())
                    );
                } else {
                    channel.basicNack(tag, false, true);
                    mqMsgLogService.update(new UpdateWrapper<MqMsgLogEntity>()
                            .eq("msg_id", correlationId)
                            .set("try_count", mqMsg.getTryCount() + 1)
                            .set("update_time", new Date())
                    );
                }
                return null;
            }
        });
    }

    /**
     * 获取CorrelationId
     *
     * @param message message
     * @return （生产者生成）消息id
     */
    public String getCorrelationId(Message message) {
        MessageProperties messageProperties = message.getMessageProperties();
        return messageProperties.getCorrelationId();
    }

    /**
     * 消息是否已被消费(幂等性)
     *
     * @param correlationId ack消息id
     * @return boolean
     */
    private boolean isConsumed(String correlationId) {
        // 查看数据库是否存在
        MqMsgLogEntity msgLog = mqMsgLogService.getOne(new QueryWrapper<MqMsgLogEntity>()
                .eq("msg_id", correlationId)
        );
        // 存在记录，且状态为已消费
        return msgLog != null && Constant.MsgLogStatus.CONSUMED_SUCCESS.equals(msgLog.getStatus());
    }

}
