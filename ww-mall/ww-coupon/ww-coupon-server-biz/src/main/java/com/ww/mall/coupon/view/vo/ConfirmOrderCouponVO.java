package com.ww.mall.coupon.view.vo;

import lombok.Data;

import java.util.List;

/**
 * @author ww
 * @create 2025-03-12- 15:14
 * @description:
 */
@Data
public class ConfirmOrderCouponVO {

    private List<OrderMemberCouponVO> availableIntegralCouponList;

    private List<OrderMemberCouponVO> availableCashCouponList;

    private List<OrderMemberCouponVO> unAvailableIntegralCouponList;

    private List<OrderMemberCouponVO> unAvailableCashCouponList;

}
