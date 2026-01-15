package com.ww.mall.coupon.service.strategy;

public interface DefaultCouponSelectStrategy {

    /**
     * 根据上下文选择默认勾选的优惠券列表
     */
    SelectionResult select(SelectionContext context);
}
