package com.ww.mall.coupon.view.vo;

import com.ww.mall.coupon.eunms.CouponDiscountType;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author ww
 * @create 2025-03-12- 09:21
 * @description: 优惠券标签信息
 */
@Data
public class CouponActivityTagVO {

    /**
     * 优惠券优惠类型
     */
    private CouponDiscountType couponDiscountType;

    /**
     * 优惠券需满X金额
     */
    private BigDecimal achieveAmount;

    /**
     * 优惠券扣减金额【折扣】
     */
    private BigDecimal deductionAmount;

}
