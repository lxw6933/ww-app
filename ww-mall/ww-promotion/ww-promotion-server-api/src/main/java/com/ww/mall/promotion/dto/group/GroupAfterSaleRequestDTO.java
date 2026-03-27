package com.ww.mall.promotion.dto.group;

import com.ww.mall.promotion.enums.GroupAfterSaleScene;
import com.ww.mall.promotion.enums.GroupTradeType;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 拼团售后处理请求。
 * <p>
 * 订单域在以下两类场景会调用该接口：
 * 1. OPEN 拼团中的订单发起售后，需要先驱动拼团释放名额/关团，再触发退款消息；
 * 2. 开团或参团在支付后被拼团规则拒绝，需要跳过拼团售后脚本，直接触发退款消息。
 *
 * @author ww
 * @create 2026-03-27
 * @description: 拼团售后统一入参，显式区分“OPEN 售后”和“支付后入团异常退款”两类场景
 */
@Data
public class GroupAfterSaleRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 售后处理场景。
     */
    private GroupAfterSaleScene scene;

    /**
     * 拼团 ID。
     */
    private String groupId;

    /**
     * 活动 ID。
     * <p>
     * 创建团异常退款时推荐透传，便于退款消息审计；OPEN 售后时允许为空，服务端会优先从拼团快照补齐。
     */
    private String activityId;

    /**
     * 用户 ID。
     */
    private Long userId;

    /**
     * 订单 ID。
     */
    private String orderId;

    /**
     * 售后单号。
     * <p>
     * OPEN 拼团售后场景下建议必传，用于拼团侧记录成员售后轨迹并支持重试去重。
     */
    private String afterSaleId;

    /**
     * 开团/参团业务类型。
     * <p>
     * 仅在“支付后入团异常退款”场景下必传，用于区分创建团退款还是参团退款。
     */
    private GroupTradeType tradeType;

    /**
     * 退款金额。
     */
    private BigDecimal refundAmount;

    /**
     * 售后或退款原因。
     */
    private String reason;

    /**
     * 事件发生时间。
     */
    private Date eventTime;
}
