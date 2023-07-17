package com.ww.mall.rabbitmq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/15 22:21
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MallCorrelationData extends CorrelationData {

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
