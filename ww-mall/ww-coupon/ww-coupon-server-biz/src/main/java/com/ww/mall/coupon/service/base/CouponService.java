package com.ww.mall.coupon.service.base;

import com.ww.mall.coupon.enums.CouponType;

/**
 * @author ww
 * @create 2026-01-13 14:04
 * @description:
 */
public interface CouponService {

    default boolean isPlatformCoupon(CouponType couponType) {
        return CouponType.PLATFORM.equals(couponType);
    }

    default boolean isMerchantCoupon(CouponType couponType) {
        return CouponType.MERCHANT.equals(couponType);
    }

}
