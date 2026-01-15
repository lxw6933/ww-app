package com.ww.mall.coupon.service.strategy;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 用户选券偏好（平台券/商家券）
 */
@Getter
public class SelectionPreference {

    /**
     * 是否使用平台优惠券
     */
    private final Boolean usePlatformCoupon;
    /**
     * 用户指定的平台优惠券活动编码
     */
    private final String selectedPlatformActivityCode;
    /**
     * 是否使用商家优惠券
     */
    private final Boolean useMerchantCoupon;
    /**
     * 用户指定的商家优惠券活动编码（merchantId -> activityCode）
     */
    private final Map<Long, String> selectedMerchantActivityCodeMap;

    @Builder
    private SelectionPreference(Boolean usePlatformCoupon,
                                String selectedPlatformActivityCode,
                                Boolean useMerchantCoupon,
                                Map<Long, String> selectedMerchantActivityCodeMap) {
        this.usePlatformCoupon = usePlatformCoupon;
        this.selectedPlatformActivityCode = selectedPlatformActivityCode;
        this.useMerchantCoupon = useMerchantCoupon;
        this.selectedMerchantActivityCodeMap = selectedMerchantActivityCodeMap;
    }
}
