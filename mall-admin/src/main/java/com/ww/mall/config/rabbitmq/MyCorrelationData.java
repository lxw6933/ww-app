package com.ww.mall.config.rabbitmq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;

/**
 * @description:
 * @author: ww
 * @create: 2021/7/1 下午12:56
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyCorrelationData extends CorrelationData {

    /**
     * 消息体
     */
    private volatile Object message;
    /**
     * 交换机
     */
    private String exchange;
    /**
     * 路由键
     */
    private String routingKey;
    /**
     * 重试次数
     */
    private Integer retryCount;

}
