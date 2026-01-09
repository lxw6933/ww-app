package com.ww.mall.promotion.mq;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author ww
 * @create 2025-12-08 19:10
 * @description: 拼团退款消息
 */
@Data
public class GroupRefundMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 拼团实例ID
     */
    private String groupId;

    /**
     * 活动ID
     */
    private String activityId;

    /**
     * 退款原因
     */
    private String reason;

    /**
     * 退款订单列表
     */
    private List<RefundOrder> refundOrders;

    @Data
    public static class RefundOrder implements Serializable {
        /**
         * 用户ID
         */
        private Long userId;

        /**
         * 订单ID
         */
        private String orderId;

        /**
         * 退款金额
         */
        private BigDecimal refundAmount;

        /**
         * 是否团长
         */
        private Boolean isLeader;
    }

}




