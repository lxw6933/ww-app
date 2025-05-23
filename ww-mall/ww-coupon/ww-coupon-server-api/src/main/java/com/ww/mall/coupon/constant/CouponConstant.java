package com.ww.mall.coupon.constant;

/**
 * @author ww
 * @create 2025-03-06- 18:20
 * @description: 优惠券常量类
 */
public class CouponConstant {

    public static final int ACTIVITY_MAX_NUMBER = 500000;

    public static final int DEFAULT_BATCH_NO = 1;

    public static final String DEFAULT_CODE = "end";

    public enum Type {
        ALL,
        CASH,
        INTEGRAL,
    }

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

    public enum ActivityStatus {
        ALL,
        WAIT_EFFECTIVE,
        EFFECTIVE,
        EXPIRED,
    }

    public enum Disabled {
        // 未到可用时间
        UN_REACHED_TIME,
        // 没有适用商品
        NO_PRODUCT,
        // 抵扣金额为0
        DISCOUNT_ZERO
    }

    public enum EffectTimeUnit {
        // 多少分钟有效期
        MINUTES,
        // 多少天有效期
        DAY
    }

}
