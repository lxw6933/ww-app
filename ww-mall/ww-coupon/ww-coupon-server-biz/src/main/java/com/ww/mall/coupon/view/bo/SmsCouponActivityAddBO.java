package com.ww.mall.coupon.view.bo;

import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.entity.SmsCouponActivity;
import com.ww.mall.coupon.eunms.*;
import com.ww.mall.coupon.utils.CouponUtils;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
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

    @NotNull(message = "渠道id不能为空")
    private Long channelId;

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
    private Date receiveStartTime;

    /**
     * 领取结束时间
     */
    @NotNull(message = "领取结束时间不能为空")
    private Date receiveEndTime;

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
     * 有效单位【天，分】【根据领取时间计算】
     */
    private CouponConstant.EffectTimeUnit effectTimeUnit;

    /**
     * 有效期【根据领取时间计算】
     */
    private int effectNumber;

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
    @Min(value = 1, message = "最小数量不能小于1")
    private int limitReceiveNumber;

    /**
     * 优惠券数量
     */
    @Min(value = 1, message = "最小数量不能小于1")
    @Max(value = 10000, message = "最大数量不能超过10000")
    private int number;

    /**
     * 活动说明描述
     */
    @NotBlank(message = "活动说明不能为空")
    private String desc;

    /**
     * 将SmsCouponActivityAddBO转换为SmsCouponActivity
     *
     * @return SmsCouponActivity
     */
    public SmsCouponActivity convertSmsCouponActivity() {
        SmsCouponActivity activity = new SmsCouponActivity();
        activity.setName(this.getName());
        activity.setDesc(this.getDesc());
        activity.setCouponDiscountType(this.getCouponDiscountType());
        activity.setAchieveAmount(this.getAchieveAmount());
        activity.setDeductionAmount(this.getDeductionAmount());
        activity.setReceiveStartTime(this.getReceiveStartTime());
        activity.setReceiveEndTime(this.getReceiveEndTime());
        activity.setEffectTimeType(this.getEffectTimeType());
        activity.setUseStartTime(this.getUseStartTime());
        activity.setUseEndTime(this.getUseEndTime());
        activity.setReceiveDay(this.getReceiveDay());
        activity.setEffectTimeUnit(this.getEffectTimeUnit());
        activity.setEffectNumber(this.getEffectNumber());
        activity.setIssueType(this.getIssueType());
        activity.setApplyMemberType(this.getApplyMemberType());
        activity.setApplyProductRangeType(this.getApplyProductRangeType());
        activity.setIdList(this.getIdList());
        activity.setLimitReceiveTimeType(this.getLimitReceiveTimeType());
        activity.setLimitReceiveNumber(this.getLimitReceiveNumber());
        activity.setNumber(this.getNumber());

        // 设置默认值
        activity.setActivityCode(CouponUtils.getSmsCouponCode());
        activity.setStatus(false);
        activity.setChannelId(this.getChannelId());
        activity.setReceiveNumber(0);
        activity.setUseNumber(0);

        return activity;
    }

}
