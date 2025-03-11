package com.ww.mall.coupon.view.vo;

import com.ww.mall.coupon.eunms.ApplyProductRangeType;
import com.ww.mall.coupon.eunms.CouponDiscountType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author ww
 * @create 2025-03-11- 13:57
 * @description:
 */
@Data
public class CouponActivityCenterVO {

    private String id;

    /**
     * 活动编码
     */
    private String activityCode;

    /**
     * 活动名称
     */
    private String title;

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
     * 开始领取时间
     */
    private Date receiveStartTime;

    /**
     * 结束领取时间
     */
    private Date receiveEndTime;

    /**
     * 适用范围
     */
    private ApplyProductRangeType applyProductRangeType;

    /**
     * 适用范围id集合
     */
    private List<Long> idList;

    /**
     * 活动说明描述
     */
    private String desc;

    /**
     * 领取比例
     */
    private BigDecimal ratio;

}
