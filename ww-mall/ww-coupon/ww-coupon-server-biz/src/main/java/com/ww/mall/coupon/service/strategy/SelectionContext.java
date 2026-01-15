package com.ww.mall.coupon.service.strategy;

import com.ww.mall.coupon.view.bo.OrderMemberSmsCouponBO;
import com.ww.mall.coupon.view.vo.OrderMemberCouponVO;
import lombok.Builder;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;
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
     * 用户选券偏好
     */
    private final SelectionPreference preference;

    @Builder
    private SelectionContext(List<OrderMemberCouponVO> platformAvailable,
                             List<OrderMemberSmsCouponBO> orderBOList,
                             Comparator<OrderMemberCouponVO> couponComparator,
                             Function<List<OrderMemberSmsCouponBO>, CouponBucket> merchantBucketProvider,
                             SelectionPreference preference) {
        this.platformAvailable = platformAvailable;
        this.orderBOList = orderBOList;
        this.couponComparator = couponComparator;
        this.merchantBucketProvider = merchantBucketProvider;
        this.preference = preference;
    }
}
