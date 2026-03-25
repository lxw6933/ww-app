package com.ww.mall.promotion.mq;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 拼团状态变更内部消息。
 * <p>
 * 该消息只用于驱动拼团域内部的异步落库链路，不直接暴露给下游业务系统。
 * 主链路在 Redis Lua 状态流转完成后，只投递这一条消息，由消费者继续执行
 * Mongo 投影同步，避免把落库副作用堆在同步方法里。
 *
 * @author ww
 * @create 2026-03-25
 * @description: 拼团状态变更内部消息
 */
@Data
public class GroupStateChangedMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 拼团ID。
     */
    private String groupId;

    /**
     * 本次状态变更发生时间。
     */
    private Date eventTime;
}
