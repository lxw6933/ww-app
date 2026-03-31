package com.ww.mall.promotion.mq;

import com.ww.mall.promotion.enums.GroupTradeType;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 拼团支付成功消息。
 * <p>
 * 支付域在订单支付成功后投递该消息，拼团域基于订单ID决定开团还是参团。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 拼团支付成功消息
 */
@Data
public class GroupOrderPaidMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务类型：START-开团，JOIN-参团。
     */
    private GroupTradeType tradeType;

    /**
     * 活动ID。
     * <p>
     * 开团场景必填，参团场景允许为空。
     */
    private String activityId;

    /**
     * 拼团ID。
     * <p>
     * 由订单域在下单阶段生成并贯穿订单、支付、售后链路；
     * 开团、参团场景都必须透传。
     */
    private String groupId;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 订单ID。
     */
    private String orderId;

    /**
     * 分享SPU ID。
     */
    private Long spuId;

    /**
     * 实际成交SKU ID。
     */
    private Long skuId;

    /**
     * 订单快照。
     */
    private String orderInfo;

    /**
     * 支付成功时间。
     */
    private Date payTime;
}
