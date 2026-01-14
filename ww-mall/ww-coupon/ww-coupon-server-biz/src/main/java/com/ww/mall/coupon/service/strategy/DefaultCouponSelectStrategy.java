package com.ww.mall.coupon.service.strategy;

import com.ww.mall.coupon.view.vo.OrderMemberCouponVO;

import java.util.List;

public interface DefaultCouponSelectStrategy {

    /**
     * 根据上下文选择默认勾选的优惠券列表
     */
    List<OrderMemberCouponVO> select(SelectionContext context);
}
