package com.ww.mall.promotion.mq;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 拼团售后成功消息。
 * <p>
 * 订单域在售后申请成功后投递该消息，拼团域会在团未成功时归还名额，
 * 并记录用户轨迹，确保后台客服可以完整回放团内过程。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 拼团售后成功消息
 */
@Data
public class GroupAfterSaleSuccessMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 售后单号。
     */
    private String afterSaleId;

    /**
     * 业务订单ID。
     */
    private String orderId;

    /**
     * 拼团ID。
     * <p>
     * 由订单域直接透传，拼团域不再按订单号反查所属拼团。
     */
    private String groupId;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 原因说明。
     */
    private String reason;

    /**
     * 售后成功时间。
     */
    private Date successTime;
}
