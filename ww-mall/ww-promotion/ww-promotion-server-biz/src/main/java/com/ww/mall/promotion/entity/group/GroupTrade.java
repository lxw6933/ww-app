package com.ww.mall.promotion.entity.group;

import com.ww.app.mongodb.common.BaseDoc;
import com.ww.mall.promotion.enums.GroupTradeStatus;
import com.ww.mall.promotion.enums.GroupTradeType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Date;

/**
 * 拼团支付回调交易单。
 * <p>
 * 用于承接支付服务的异步回调，记录 payTransId、orderId 与 groupId 的映射，
 * 避免支付重复通知时重复开团或重复参团。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 拼团支付回调交易单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document("group_trade")
public class GroupTrade extends BaseDoc {

    /**
     * 链路追踪ID。
     */
    private String traceId;

    /**
     * 业务类型。
     */
    private GroupTradeType tradeType;

    /**
     * 交易状态。
     */
    private GroupTradeStatus status;

    /**
     * 活动ID。
     */
    private String activityId;

    /**
     * 拼团ID。
     * <p>
     * 成功落团后会被回填，可用于通过交易单直接回放最终拼团结果。
     */
    @Indexed
    private String groupId;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 订单ID。
     * <p>
     * 业务订单在拼团域内要求全局唯一，重复回调时优先作为幂等兜底键。
     */
    @Indexed(unique = true)
    private String orderId;

    /**
     * 支付流水ID。
     * <p>
     * 支付服务异步回调的首选幂等键，应保持全局唯一。
     */
    @Indexed(unique = true)
    private String payTransId;

    /**
     * 订单信息快照。
     */
    private String orderInfo;

    /**
     * 支付回调时间。
     */
    private Date callbackTime;

    /**
     * 失败原因。
     */
    private String failReason;

    /**
     * 按支付流水ID查询。
     *
     * @param payTransId 支付流水ID
     * @return 查询对象
     */
    public static Query buildPayTransIdQuery(String payTransId) {
        return new Query().addCriteria(Criteria.where("payTransId").is(payTransId));
    }

    /**
     * 按订单ID查询。
     *
     * @param orderId 订单ID
     * @return 查询对象
     */
    public static Query buildOrderIdQuery(String orderId) {
        return new Query().addCriteria(Criteria.where("orderId").is(orderId));
    }
}
