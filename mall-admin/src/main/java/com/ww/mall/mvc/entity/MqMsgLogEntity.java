package com.ww.mall.mvc.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ww.mall.common.constant.Constant;
import com.ww.mall.common.utils.DateUtils;
import com.ww.mall.common.utils.JsonUtils;
import com.ww.mall.mvc.base.BaseNoRecorderEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @description: MQ消费记录表
 * @author: ww
 * @create: 2021/6/30 上午9:36
 **/
@Data
@NoArgsConstructor
@TableName("tb_mq_msg_log")
@EqualsAndHashCode(callSuper = true)
public class MqMsgLogEntity extends BaseNoRecorderEntity {

    @TableId
    private Long id;

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
    @TableLogic
    private Boolean isDel;

    /**
     * 乐观锁
     */
    @Version
    private Integer version;

    public MqMsgLogEntity(Object message, String exchange, String routingKey) {
        this.message = JsonUtils.toJson(message);
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.status = Constant.MsgLogStatus.DELIVERING;
        this.nextTryTime = (DateUtils.nextMinute(1));
    }

}
