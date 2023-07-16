package com.ww.mall.rabbitmq;

import com.alibaba.fastjson.JSON;
import com.ww.mall.common.constant.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/15 23:42
 **/
@Data
@NoArgsConstructor
@Document("t_mq_msg_log")
public class MqMsgLogEntity {

    @Id
    private String id;

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
     * 消息状态(0投递中 1投递成功 2投递失败 3已消费 4消费失败)
     */
    private Integer status;

    /**
     * 重试次数
     */
    private Integer tryCount;

    /**
     * 下一次重试时间
     */
    private Date nextTryTime;

    /**
     * 是否删除（0：否，1：是）
     */
    private Boolean isDel;

    /** 创建时间 */
    private String createTime;

    /** 修改时间 */
    private String updateTime;

    public MqMsgLogEntity(Object message, String exchange, String routingKey) {
        this.message = JSON.toJSONString(message);
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.status = Constant.MsgLogStatus.DELIVERING;
        this.nextTryTime = (DateUtils.addMinutes(new Date(), 1));
    }

}
