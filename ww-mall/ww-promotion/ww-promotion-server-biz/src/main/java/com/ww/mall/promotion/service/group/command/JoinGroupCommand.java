package com.ww.mall.promotion.service.group.command;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 参团命令。
 * <p>
 * 该对象仅在拼团服务内部流转，承接支付成功后的参团编排参数，
 * 避免业务层继续依赖 Controller 请求模型。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 拼团内部参团命令
 */
@Data
public class JoinGroupCommand {

    /**
     * 拼团实例ID。
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
     * 实际成交SKU ID。
     */
    private Long skuId;

    /**
     * 订单快照。
     */
    private String orderInfo;

    /**
     * 支付金额。
     */
    private BigDecimal payAmount;
}
