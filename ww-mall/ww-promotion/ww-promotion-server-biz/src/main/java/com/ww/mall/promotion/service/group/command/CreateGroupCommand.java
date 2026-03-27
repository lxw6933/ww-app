package com.ww.mall.promotion.service.group.command;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 开团命令。
 * <p>
 * 该对象仅在拼团服务内部流转，承接支付成功后的开团编排参数，
 * 避免业务层继续依赖 Controller 请求模型。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 拼团内部开团命令
 */
@Data
public class CreateGroupCommand {

    /**
     * 拼团实例ID。
     * <p>
     * 由上游订单域在下单时预生成并透传，
     * 拼团域直接以该值作为开团业务主键。
     */
    private String groupId;

    /**
     * 活动ID。
     */
    private String activityId;

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
     * 支付金额。
     */
    private BigDecimal payAmount;
}
