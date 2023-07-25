package com.ww.mall.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.mall.coupon.eunms.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author ww
 * @create 2023-07-25- 09:03
 * @description:
 */

@Data
@TableName("t_coupon")
@EqualsAndHashCode(callSuper = true)
public class Coupon extends BaseEntity {

    /**
     * 活动唯一编码
     */
    private String activityCode;

    /**
     * 渠道id
     */
    private Long channelId;

    /**
     * 商家id
     */
    private Long merchantId;

    /**
     * 优惠券名称
     */
    private String title;

    /**
     * 优惠券类型【平台、商家】
     */
    private CouponType couponType;

    /**
     * 优惠券优惠类型【满减券、代金券、满折券】
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
     * 领取开始时间
     */
    private Date receiveStartTime;

    /**
     * 领取结束时间
     */
    private Date receiveEndTime;

    /**
     * 使用时间类型
     */
    private CouponUseTimeType couponUseTimeType;

    /**
     * 优惠券允许使用开始时间【CouponUseTimeType】
     */
    private Date useStartTime;

    /**
     * 优惠券允许使用结束时间【CouponUseTimeType】
     */
    private Date useEndTime;

    /**
     * 指定领取后多少天数后可用【CouponUseTimeType】
     */
    private Integer receiveAfterDayEffect;

    /**
     * 指定领取后达到使用时间后，多少天过期【CouponUseTimeType】
     */
    private Integer receiveAfterEffectDay;

    /**
     * 初始化多少张优惠券
     */
    private Integer initTotalCouponNumber;

    /**
     * 是否初始化完成
     */
    private Boolean initSuccess;

    /**
     * 优惠券领取限制时间类型【月、周、日、永久】
     */
    private CouponLimitReceiveTimeType couponLimitReceiveTimeType;

    /**
     * 优惠券在限制时间范围内允许领取的数量
     */
    private Integer couponLimitReceiveNumber;

    /**
     * 客群类型【指定用户待扩展】
     */
    private AllowMemberRangeType allowMemberRangeType;

    /**
     * 优惠券发放类型【用户领取、手动发放】
     */
    private CouponDistributeType couponDistributeType;

    /**
     * 适用商品类型
     */
    private AllowProductRangeType allowProductRangeType;

    /**
     * 上下架状态
     */
    private Boolean state;

    /**
     * 详细说明
     */
    private String remark;

}
