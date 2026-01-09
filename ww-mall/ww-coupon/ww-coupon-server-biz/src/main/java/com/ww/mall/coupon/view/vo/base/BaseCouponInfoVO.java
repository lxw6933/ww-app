package com.ww.mall.coupon.view.vo.base;

import com.ww.mall.coupon.enums.CouponDiscountType;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author ww
 * @create 2025-03-12- 10:49
 * @description:
 */
@Data
public class BaseCouponInfoVO {

    private String id;

    /**
     * 优惠券唯一活动编码
     */
    private String activityCode;

    /**
     * 活动名称
     */
    private String name;

    /**
     * 活动说明描述
     */
    private String desc;

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
