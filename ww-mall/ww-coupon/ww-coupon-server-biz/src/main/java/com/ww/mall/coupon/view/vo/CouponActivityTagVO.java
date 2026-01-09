package com.ww.mall.coupon.view.vo;

import com.ww.mall.coupon.enums.CouponDiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author ww
 * @create 2025-03-12- 09:21
 * @description: 优惠券标签信息
 */
@Data
@Schema(description = "优惠券标签信息")
public class CouponActivityTagVO {

    /**
     * 优惠券优惠类型
     */
    @Schema(description = "优惠券优惠类型", example = "DIRECT_REDUCTION")
    private CouponDiscountType couponDiscountType;

    /**
     * 优惠券需满X金额
     */
    @Schema(description = "优惠券需满金额", example = "100.00")
    private BigDecimal achieveAmount;

    /**
     * 优惠券扣减金额【折扣】
     */
    @Schema(description = "优惠券扣减金额或折扣率", example = "20.00")
    private BigDecimal deductionAmount;

}
