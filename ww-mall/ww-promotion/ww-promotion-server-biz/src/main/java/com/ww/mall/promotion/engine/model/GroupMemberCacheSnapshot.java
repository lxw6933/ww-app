package com.ww.mall.promotion.engine.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 拼团成员 Redis 轻量快照。
 * <p>
 * 该模型只服务于 Redis member-store 存储，目标是压缩主链路缓存体积：
 * 1. 只保留状态机推进、退款补偿、查询展示必需字段。
 * 2. 不复用 Mongo 实体，避免 BaseDoc 与历史兼容字段一并写入 Redis。
 * 3. 时间统一使用毫秒值，降低 JSON 体积与序列化成本。
 *
 * @author ww
 * @create 2026-03-19
 * @description: 拼团成员 Redis 轻量快照
 */
@Data
public class GroupMemberCacheSnapshot {

    /**
     * 拼团实例ID。
     */
    private String groupInstanceId;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 订单ID。
     */
    private String orderId;

    /**
     * 入团时间毫秒值。
     */
    private Long joinTime;

    /**
     * 实际支付金额。
     */
    private BigDecimal payAmount;

    /**
     * 实际成交 SKU ID。
     */
    private Long skuId;

    /**
     * 成员业务状态。
     */
    private String memberStatus;

    /**
     * 售后单号。
     */
    private String afterSaleId;

}
