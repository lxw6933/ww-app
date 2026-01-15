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

    @Schema(description = "平台优惠券分层（可用/不可用）")
    private CouponScopeVO platform;

    @Schema(description = "商家优惠券分层（可用/不可用）")
    private CouponScopeVO merchant;

    @Schema(description = "默认选中优惠券列表")
    private List<OrderMemberCouponVO> selectedCouponList;

    @Data
    @Schema(description = "优惠券分层（可用/不可用）")
    public static class CouponScopeVO {
        @Schema(description = "可用优惠券")
        private CouponGroupVO available;

        @Schema(description = "不可用优惠券")
        private CouponGroupVO unavailable;
    }

    @Data
    @Schema(description = "优惠券分组")
    public static class CouponGroupVO {
        @Schema(description = "积分优惠券列表")
        private List<OrderMemberCouponVO> integral;

        @Schema(description = "现金优惠券列表")
        private List<OrderMemberCouponVO> cash;
    }

}
