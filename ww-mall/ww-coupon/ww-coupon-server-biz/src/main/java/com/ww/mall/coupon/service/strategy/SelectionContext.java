package com.ww.mall.coupon.service.strategy;

import com.ww.mall.coupon.view.bo.OrderMemberSmsCouponBO;
import com.ww.mall.coupon.view.vo.OrderMemberCouponVO;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Getter
public class SelectionContext {

    /**
     * 平台可用券列表（已做适用范围与门槛校验）
     */
    private final List<OrderMemberCouponVO> platformAvailable;
    /**
     * 原始订单商品信息（可能被策略原地修改）
     */
    private final List<OrderMemberSmsCouponBO> orderBOList;
    /**
     * 默认选券排序规则
     */
    private final Comparator<OrderMemberCouponVO> couponComparator;
    /**
     * 基于订单信息生成商家券分桶结果的回调（用于平台券均摊后重新计算）
     */
    private final Function<List<OrderMemberSmsCouponBO>, CouponBucket> merchantBucketProvider;
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

    public SelectionContext(List<OrderMemberCouponVO> platformAvailable,
                            List<OrderMemberSmsCouponBO> orderBOList,
                            Comparator<OrderMemberCouponVO> couponComparator,
                            Function<List<OrderMemberSmsCouponBO>, CouponBucket> merchantBucketProvider,
                            Boolean usePlatformCoupon,
                            String selectedPlatformActivityCode,
                            Boolean useMerchantCoupon,
                            Map<Long, String> selectedMerchantActivityCodeMap) {
        this.platformAvailable = platformAvailable;
        this.orderBOList = orderBOList;
        this.couponComparator = couponComparator;
        this.merchantBucketProvider = merchantBucketProvider;
        this.usePlatformCoupon = usePlatformCoupon;
        this.selectedPlatformActivityCode = selectedPlatformActivityCode;
        this.useMerchantCoupon = useMerchantCoupon;
        this.selectedMerchantActivityCodeMap = selectedMerchantActivityCodeMap;
    }

}
