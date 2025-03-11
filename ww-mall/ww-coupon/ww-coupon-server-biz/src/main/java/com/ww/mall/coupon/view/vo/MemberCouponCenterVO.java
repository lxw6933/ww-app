package com.ww.mall.coupon.view.vo;

import com.ww.mall.coupon.eunms.CouponDiscountType;
import com.ww.mall.coupon.eunms.CouponStatus;
import com.ww.mall.coupon.eunms.CouponType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author ww
 * @create 2025-03-11- 13:57
 * @description:
 */
@Data
public class MemberCouponCenterVO {

    private String id;

    /**
     * 优惠券唯一活动编码
     */
    private String activityCode;

    /**
     * 优惠券类型【店铺、渠道】
     */
    private CouponType couponType;

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

    /**
     * 开始使用时间
     */
    private Date useStartTime;

    /**
     * 过期时间
     */
    private Date useEndTime;

    /**
     * 优惠券券码状态
     */
    private CouponStatus couponStatus;

    /**
     * 活动名称
     */
    private String title;

    /**
     * 活动说明描述
     */
    private String desc;
}
