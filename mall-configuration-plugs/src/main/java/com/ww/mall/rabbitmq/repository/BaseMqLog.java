package com.ww.mall.rabbitmq.repository;

import com.ww.mall.rabbitmq.enums.MqMsgStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.poi.ss.formula.functions.T;

import java.util.Date;

/**
 * @author ww
 * @create 2024-05-28- 16:11
 * @description:
 */
@Data
@NoArgsConstructor
public class BaseMqLog {

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

    /** 创建时间 */
    private String createTime;

    /** 修改时间 */
    private String updateTime;

    public BaseMqLog(String message, String exchange, String routingKey) {
        this.message = message;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.status = MqMsgStatus.DELIVER_SUCCESS;
        this.nextTryTime = (DateUtils.addMinutes(new Date(), 1));
    }

}
