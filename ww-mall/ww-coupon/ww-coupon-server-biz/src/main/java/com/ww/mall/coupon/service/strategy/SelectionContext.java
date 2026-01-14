package com.ww.mall.coupon.service.strategy;

import com.ww.mall.coupon.view.bo.OrderMemberSmsCouponBO;
import com.ww.mall.coupon.view.vo.OrderMemberCouponVO;
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
     * 原始订单商品信息
     */
    private final List<OrderMemberSmsCouponBO> orderBOList;
    /**
     * 默认选券排序规则
     */
    private final Comparator<OrderMemberCouponVO> couponComparator;
    /**
     * 基于订单信息生成商家可用券列表的回调（用于平台券均摊后重新计算）
     */
    private final Function<List<OrderMemberSmsCouponBO>, List<OrderMemberCouponVO>> merchantAvailableProvider;

    public SelectionContext(List<OrderMemberCouponVO> platformAvailable,
                            List<OrderMemberSmsCouponBO> orderBOList,
                            Comparator<OrderMemberCouponVO> couponComparator,
                            Function<List<OrderMemberSmsCouponBO>, List<OrderMemberCouponVO>> merchantAvailableProvider) {
        this.platformAvailable = platformAvailable;
        this.orderBOList = orderBOList;
        this.couponComparator = couponComparator;
        this.merchantAvailableProvider = merchantAvailableProvider;
    }

}
