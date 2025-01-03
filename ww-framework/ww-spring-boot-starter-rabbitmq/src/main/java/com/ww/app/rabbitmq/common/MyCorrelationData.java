package com.ww.app.rabbitmq.common;

import com.ww.app.common.thread.ThreadMdcUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.amqp.rabbit.connection.CorrelationData;

import java.util.UUID;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/15 22:21
 **/
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MyCorrelationData<T> extends CorrelationData {

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

    /**
     * true：无任何处理【消费自行保证业务幂等】
     * false：mongodb msg
     */
    private boolean msgMode;

    /**
     * 消息失败原因
     */
    private String failCause;

    public MyCorrelationData(boolean init) {
        if (init) {
            this.setId(UUID.randomUUID().toString());
            this.traceId = ThreadMdcUtil.getTraceId();
        }
    }

}
