package com.ww.mall.promotion.constants;

/**
 * 拼团模块 Bean Validation 文案常量。
 * <p>
 * 该类只服务于请求对象上的注解校验消息，和业务异常 `ResCode` 分层隔离：
 * 1. 入参缺失/格式问题由 Bean Validation 直接返回。
 * 2. 业务规则问题仍由 service 层统一抛出 `ApiException(ResCode)`。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 拼团模块注解校验文案常量
 */
public final class GroupValidationMessageConstants {

    /**
     * 活动名称不能为空。
     */
    public static final String ACTIVITY_NAME_REQUIRED = "活动名称不能为空";

    /**
     * 商品SPU ID不能为空。
     */
    public static final String SPU_ID_REQUIRED = "商品SPU ID不能为空";

    /**
     * 活动 SPU 配置不能为空。
     */
    public static final String SPU_CONFIGS_REQUIRED = "活动SPU配置不能为空";

    /**
     * 商品SKU ID不能为空。
     */
    public static final String SKU_ID_REQUIRED = "商品SKU ID不能为空";

    /**
     * SKU 规则不能为空。
     */
    public static final String SKU_RULES_REQUIRED = "SKU规则不能为空";

    /**
     * 拼团价格不能为空。
     */
    public static final String GROUP_PRICE_REQUIRED = "拼团价格不能为空";

    /**
     * 拼团价格必须大于0。
     */
    public static final String GROUP_PRICE_POSITIVE = "拼团价格必须大于0";

    /**
     * 拼团人数不能为空。
     */
    public static final String REQUIRED_SIZE_REQUIRED = "拼团人数不能为空";

    /**
     * 拼团人数必须大于0。
     */
    public static final String REQUIRED_SIZE_POSITIVE = "拼团人数必须大于0";

    /**
     * 拼团有效期不能为空。
     */
    public static final String EXPIRE_HOURS_REQUIRED = "拼团有效期不能为空";

    /**
     * 拼团有效期必须大于0。
     */
    public static final String EXPIRE_HOURS_POSITIVE = "拼团有效期必须大于0";

    /**
     * 活动开始时间不能为空。
     */
    public static final String START_TIME_REQUIRED = "活动开始时间不能为空";

    /**
     * 活动结束时间不能为空。
     */
    public static final String END_TIME_REQUIRED = "活动结束时间不能为空";

    /**
     * 活动ID不能为空。
     */
    public static final String ACTIVITY_ID_REQUIRED = "活动ID不能为空";

    /**
     * 用户ID不能为空。
     */
    public static final String USER_ID_REQUIRED = "用户ID不能为空";

    /**
     * 订单ID不能为空。
     */
    public static final String ORDER_ID_REQUIRED = "订单ID不能为空";

    /**
     * 拼团实例ID不能为空。
     */
    public static final String GROUP_ID_REQUIRED = "拼团实例ID不能为空";

    /**
     * 业务类型不能为空。
     */
    public static final String TRADE_TYPE_REQUIRED = "业务类型不能为空";

    /**
     * 支付流水ID不能为空。
     */
    public static final String PAY_TRANS_ID_REQUIRED = "支付流水ID不能为空";

    /**
     * 售后单号不能为空。
     */
    public static final String AFTER_SALE_ID_REQUIRED = "售后单号不能为空";

    private GroupValidationMessageConstants() {
    }
}
