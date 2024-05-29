package com.ww.mall.rabbitmq;

import com.ww.mall.common.constant.Constant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.connection.CorrelationData;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/15 22:21
 **/
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MallCorrelationData<T> extends CorrelationData {

    /**
     * 消息体
     */
    private T message;
    /**
     * 交换机
     */
    private String exchange;
    /**
     * 路由键
     */
    private String routingKey;
    /**
     * tracId
     */
    private String traceId;

    public MallCorrelationData() {
        this.traceId = MDC.get(Constant.TRACE_ID);
    }

}
