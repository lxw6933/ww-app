package com.ww.mall.promotion.key;

import com.ww.app.redis.key.RedisKeyBuilder;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2025-12-08 17:00
 * @description: 拼团Redis Key构建器
 */
@Component
public class GroupRedisKeyBuilder extends RedisKeyBuilder {

    private static final String GROUP = "group";
    private static final String ACTIVITY = "activity";
    private static final String INSTANCE = "instance";
    private static final String META = "meta";
    private static final String SLOTS = "slots";
    private static final String MEMBERS = "members";
    private static final String MEMBER_DETAIL = "member:detail";
    private static final String ORDERS = "orders";
    private static final String EXPIRY = "expiry";
    private static final String STOCK = "stock";
    private static final String OPEN = "open";
    private static final String USER_GROUP = "user:group";
    private static final String USER_COUNT = "user:count";
    private static final String ORDER_MAPPING = "order:mapping";
    private static final String CALLBACK = "callback";
    private static final String TRADE = "trade";
    private static final String AFTER_SALE = "after:sale";
    private static final String LOCK = "lock";

    /**
     * 拼团实例元数据Key
     * 示例：{prefix}group:instance:meta:{groupId}
     */
    public String buildGroupMetaKey(String groupId) {
        return join(GROUP, INSTANCE, META, groupId);
    }

    /**
     * 拼团剩余名额Key
     * 示例：{prefix}group:instance:slots:{groupId}
     */
    public String buildGroupSlotsKey(String groupId) {
        return join(GROUP, INSTANCE, SLOTS, groupId);
    }

    /**
     * 拼团成员Key（Sorted Set）
     * 示例：{prefix}group:instance:members:{groupId}
     */
    public String buildGroupMembersKey(String groupId) {
        return join(GROUP, INSTANCE, MEMBERS, groupId);
    }

    /**
     * 拼团成员详情Key（Hash）。
     * 示例：{prefix}group:instance:member:detail:{groupId}
     */
    public String buildGroupMemberDetailKey(String groupId) {
        return join(GROUP, INSTANCE, MEMBER_DETAIL, groupId);
    }

    /**
     * 拼团订单Key（Hash）
     * 示例：{prefix}group:instance:orders:{groupId}
     */
    public String buildGroupOrdersKey(String groupId) {
        return join(GROUP, INSTANCE, ORDERS, groupId);
    }

    /**
     * 过期索引Key（Sorted Set，score为过期时间）
     * 示例：{prefix}group:expiry
     */
    public String buildExpiryIndexKey() {
        return join(GROUP, EXPIRY);
    }

    /**
     * 活动库存Key
     * 示例：{prefix}group:activity:stock:{activityId}
     */
    public String buildActivityStockKey(String activityId) {
        return join(GROUP, ACTIVITY, STOCK, activityId);
    }

    /**
     * 活动可参团开放索引。
     * 示例：{prefix}group:activity:open:{activityId}
     */
    public String buildActivityOpenGroupKey(String activityId) {
        return join(GROUP, ACTIVITY, OPEN, activityId);
    }

    /**
     * 用户参与的拼团Key（Set）
     * 示例：{prefix}group:user:group:{userId}
     */
    public String buildUserGroupKey(Long userId) {
        return join(GROUP, USER_GROUP, String.valueOf(userId));
    }

    /**
     * 用户参与活动次数Key（Hash）
     * 示例：{prefix}group:activity:user:count:{activityId}
     */
    public String buildUserActivityCountKey(String activityId) {
        return join(GROUP, ACTIVITY, USER_COUNT, activityId);
    }

    /**
     * 订单与拼团映射Key。
     * 示例：{prefix}group:order:mapping:{orderId}
     */
    public String buildOrderMappingKey(String orderId) {
        return join(GROUP, ORDER_MAPPING, orderId);
    }

    /**
     * 支付回调锁Key。
     * 示例：{prefix}group:callback:lock:{bizKey}
     */
    public String buildPaymentCallbackLockKey(String bizKey) {
        return join(GROUP, CALLBACK, LOCK, bizKey);
    }

    /**
     * 订单处理锁Key。
     * 示例：{prefix}group:trade:lock:{bizKey}
     */
    public String buildTradeLockKey(String bizKey) {
        return join(GROUP, TRADE, LOCK, bizKey);
    }

    /**
     * 售后处理锁Key。
     * 示例：{prefix}group:after:sale:lock:{bizKey}
     */
    public String buildAfterSaleLockKey(String bizKey) {
        return join(GROUP, AFTER_SALE, LOCK, bizKey);
    }

    /**
     * 拼团实例锁Key。
     * 示例：{prefix}group:instance:lock:{groupId}
     */
    public String buildGroupLockKey(String groupId) {
        return join(GROUP, INSTANCE, LOCK, groupId);
    }

    /**
     * 使用父类前缀与统一分隔符拼接Key
     */
    private String join(String... parts) {
        return getPrefix() + String.join(SPLIT_ITEM, parts);
    }
}
