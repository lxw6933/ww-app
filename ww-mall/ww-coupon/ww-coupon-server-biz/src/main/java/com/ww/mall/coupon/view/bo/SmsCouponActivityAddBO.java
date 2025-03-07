package com.ww.mall.coupon.view.bo;

import com.ww.mall.coupon.eunms.*;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author ww
 * @create 2025-03-05- 15:07
 * @description:
 */
@Data
public class SmsCouponActivityAddBO {

    /**
     * 活动名称
     */
    @NotBlank(message = "活动名词不能为空")
    private String name;

    /**
     * 优惠券优惠类型
     */
    @NotNull(message = "优惠类型不能为空")
    private CouponDiscountType couponDiscountType;

    /**
     * 优惠券需满X金额
     */
    @NotNull(message = "满减金额不能为空")
    private BigDecimal achieveAmount;

    /**
     * 优惠券扣减金额【折扣】
     */
    @NotNull(message = "抵扣金额不能为空")
    private BigDecimal deductionAmount;

    /**
     * 领取开始时间
     */
    @NotNull(message = "领取开始时间不能为空")
    private Date receiveStartDate;

    /**
     * 领取结束时间
     */
    @NotNull(message = "领取结束时间不能为空")
    private Date receiveEndDate;

    /**
     * 优惠券领取后有效期计算类型
     * [固定有效期]
     * [根据领取时间计算]
     */
    @NotNull(message = "优惠券生效类型不能为空")
    private EffectTimeType effectTimeType;

    /**
     * 优惠券有效开始时间【固定有效期】
     */
    private Date useStartTime;

    /**
     * 优惠券有效结束时间【固定有效期】
     */
    private Date useEndTime;

    /**
     * 领取多少天后可用【根据领取时间计算】
     */
    private int receiveDay;

    /**
     * 多少天的有效期【根据领取时间计算】
     */
    private int day;

    /**
     * 发放类型
     */
    @NotNull(message = "发放类型不能为空")
    private IssueType issueType;

    /**
     * 适用用户范围
     */
    @NotNull(message = "适用用户范围不能为空")
    private ApplyMemberType applyMemberType;

    /**
     * 适用范围
     */
    @NotNull(message = "适用范围不能为空")
    private ApplyProductRangeType applyProductRangeType;

    /**
     * 适用范围id集合
     */
    private List<Long> idList;

    /**
     * 领取限制类型
     */
    @NotNull(message = "领取限制类型不能为空")
    private LimitReceiveTimeType limitReceiveTimeType;

    /**
     * 领取限制数量
     */
    private int limitReceiveNumber;

    /**
     * 优惠券数量
     */
    private int number;

    /**
     * 活动说明描述
     */
    @NotBlank(message = "活动说明不能为空")
    private String desc;

}
