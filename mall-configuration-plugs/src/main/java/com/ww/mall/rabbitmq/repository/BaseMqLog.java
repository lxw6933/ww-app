package com.ww.mall.rabbitmq.repository;

import com.ww.mall.mongodb.BaseDoc;
import com.ww.mall.rabbitmq.enums.MqMsgStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author ww
 * @create 2024-05-28- 16:11
 * @description:
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BaseMqLog extends BaseDoc {

    /**
     * ack消息id
     */
    private String msgId;

    /**
     * 发送信息体
     */
    private String message;

    /**
     * 交换机
     */
    private String exchange;

    /**
     * 路由key
     */
    private String routingKey;

    /**
     * 消息状态
     */
    private MqMsgStatus status;

    /**
     * 重试次数
     */
    private Integer tryCount;

    /**
     * 下一次重试时间
     */
    private Date nextTryTime;

}
