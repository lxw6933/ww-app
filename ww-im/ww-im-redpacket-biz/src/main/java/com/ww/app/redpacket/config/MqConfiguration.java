package com.ww.app.redpacket.config;

import com.ww.app.redpacket.common.RedpacketMQConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ww
 * @create 2024-12-30- 16:07
 * @description:
 */
@Configuration
public class MqConfiguration {

    /**
     * 红包延时交换机初始化
     */
    @Bean(name = RedpacketMQConstant.REDPACKET_DELAY_EXCHANGE)
    public CustomExchange redpacketDelayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "topic");
        return new CustomExchange(RedpacketMQConstant.REDPACKET_DELAY_EXCHANGE, "x-delayed-message",true, false, args);
    }

    /**
     * 初始化红包回滚队列
     */
    @Bean(name = RedpacketMQConstant.REDPACKET_ROLLBACK_QUEUE)
    public Queue redpacketRollbackQueue() {
        return new Queue(RedpacketMQConstant.REDPACKET_ROLLBACK_QUEUE);
    }

    /**
     * 绑定回滚交换机和队列关系
     */
    @Bean
    public Binding redpacketRollbackQueueBind() {
        return BindingBuilder.bind(redpacketRollbackQueue()).to(redpacketDelayExchange()).with(RedpacketMQConstant.REDPACKET_ROLLBACK_KEY).noargs();
    }

}
