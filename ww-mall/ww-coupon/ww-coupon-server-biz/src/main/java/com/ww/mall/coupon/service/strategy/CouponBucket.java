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

}
