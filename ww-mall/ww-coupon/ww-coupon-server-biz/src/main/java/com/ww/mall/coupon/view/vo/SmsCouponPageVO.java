package com.ww.mall.coupon.view.vo;

import com.ww.mall.coupon.eunms.CouponDiscountType;
import com.ww.mall.coupon.eunms.IssueType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author ww
 * @create 2023-07-26- 09:16
 * @description:
 */
@Data
public class SmsCouponPageVO {

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
     * 发放类型
     */
    private IssueType issueType;

    /**
     * 初始化优惠券数量
     */
    private Integer number;

    /**
     * 优惠券剩余可用数量
     */
    private Integer availableNumber;

    /**
     * 已领取数量
     */
    private Integer receiveNumber;

    /**
     * 已使用数量
     */
    private Integer usedNumber;

    /**
     * 开始领取时间
     */
    private Date receiveStartTime;

    /**
     * 结束领取时间
     */
    private Date receiveEndTime;

    /**
     * 上下架状态
     */
    private Boolean status;

    /**
     * 创建时间
     */
    private String createTime;

}
