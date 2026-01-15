package com.ww.mall.coupon.service.strategy;

import com.ww.mall.coupon.view.vo.OrderMemberCouponVO;
import lombok.Getter;

import java.util.List;

@Getter
public class SelectionResult {

    /**
     * 默认选中的优惠券列表（平台券 + 商家券）
     */
    private final List<OrderMemberCouponVO> selectedCoupons;

    /**
     * 商家券分桶结果（用于展示可用/不可用列表）
     */
    private final CouponBucket merchantBucket;

    public SelectionResult(List<OrderMemberCouponVO> selectedCoupons, CouponBucket merchantBucket) {
        this.selectedCoupons = selectedCoupons;
        this.merchantBucket = merchantBucket;
    }

}
