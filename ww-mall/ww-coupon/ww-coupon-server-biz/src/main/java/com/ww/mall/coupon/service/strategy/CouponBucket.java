package com.ww.mall.coupon.service.strategy;

import com.ww.mall.coupon.view.vo.OrderMemberCouponVO;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CouponBucket {

    private final List<OrderMemberCouponVO> availableIntegralList = new ArrayList<>();
    private final List<OrderMemberCouponVO> availableCashList = new ArrayList<>();
    private final List<OrderMemberCouponVO> unAvailableIntegralList = new ArrayList<>();
    private final List<OrderMemberCouponVO> unAvailableCashList = new ArrayList<>();

    /**
     * 汇总所有可用券（积分券 + 现金券）
     */
    public List<OrderMemberCouponVO> buildAllAvailableList() {
        List<OrderMemberCouponVO> allAvailableList = new ArrayList<>();
        allAvailableList.addAll(this.availableIntegralList);
        allAvailableList.addAll(this.availableCashList);
        return allAvailableList;
    }

}
