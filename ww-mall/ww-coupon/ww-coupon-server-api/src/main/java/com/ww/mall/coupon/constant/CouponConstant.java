package com.ww.mall.coupon.constant;

/**
 * @author ww
 * @create 2025-03-06- 18:20
 * @description: 优惠券常量类
 */
public class CouponConstant {

    public static final String DEFAULT_BATCH_NO = "0";

    public static final String DEFAULT_CODE = "end";

    public enum Status {
        // 全部
        ALL,
        // 可使用
        USE,
        // 已使用
        USED,
        // 已过期
        EXPIRE
    }

    public enum Disabled {
        // 未到可用时间
        UN_REACHED_TIME,
        // 没有适用商品
        NO_PRODUCT,
        // 抵扣金额为0
        DISCOUNT_ZERO
    }

}
