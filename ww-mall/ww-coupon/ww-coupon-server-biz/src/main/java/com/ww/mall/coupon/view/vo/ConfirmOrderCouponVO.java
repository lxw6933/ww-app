package com.ww.mall.coupon.view.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author ww
 * @create 2025-03-12- 15:14
 * @description:
 */
@Data
@Schema(description = "确认下单优惠券信息")
public class ConfirmOrderCouponVO {

    @Schema(description = "可用积分优惠券列表")
    private List<OrderMemberCouponVO> availableIntegralCouponList;

    @Schema(description = "可用现金优惠券列表")
    private List<OrderMemberCouponVO> availableCashCouponList;

    @Schema(description = "不可用积分优惠券列表")
    private List<OrderMemberCouponVO> unAvailableIntegralCouponList;

    @Schema(description = "不可用现金优惠券列表")
    private List<OrderMemberCouponVO> unAvailableCashCouponList;

}
