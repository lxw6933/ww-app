package com.ww.app.coupon.view.vo;

import com.ww.app.coupon.eunms.CouponActivityStatus;
import com.ww.app.coupon.eunms.CouponDiscountType;
import com.ww.app.coupon.eunms.CouponType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author ww
 * @create 2023-07-26- 09:16
 * @description:
 */
@Data
public class CouponPageVO {

    /**
     * 活动编码
     */
    private String activityCode;

    /**
     * 活动名称
     */
    private String title;

    /**
     * 优惠券类型
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
     * 初始化优惠券数量
     */
    private Integer initTotalCouponNumber;

    /**
     * 已领取数量
     */
    private Integer receiveCouponNumber;

    /**
     * 已使用数量
     */
    private Integer usedCouponNumber;

    /**
     * 开始领取时间
     */
    private Date receiveStartTime;

    /**
     * 结束领取时间
     */
    private Date receiveEndTime;

    /**
     * 活动状态
     */
    private CouponActivityStatus couponActivityStatus;

    /**
     * 上下架状态
     */
    private Boolean state;

}
