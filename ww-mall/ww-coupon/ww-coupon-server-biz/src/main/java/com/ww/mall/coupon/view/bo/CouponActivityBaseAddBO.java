package com.ww.mall.coupon.view.bo;

import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.entity.base.BaseCouponInfo;
import com.ww.mall.coupon.enums.*;
import com.ww.mall.coupon.utils.CouponUtils;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "优惠券活动基础新增参数")
public class CouponActivityBaseAddBO {

    /**
     * 活动名称
     */
    @Schema(description = "活动名称", example = "新用户专享优惠券")
    @NotBlank(message = "活动名称不能为空")
    private String name;

    /**
     * 优惠券优惠类型
     */
    @Schema(description = "优惠券优惠类型", example = "DIRECT_REDUCTION", allowableValues = {"DIRECT_REDUCTION", "FULL_REDUCTION", "FULL_DISCOUNT", "INTEGRAL_DISCOUNT"})
    @NotNull(message = "优惠类型不能为空")
    private CouponDiscountType couponDiscountType;

    /**
     * 优惠券需满X金额
     */
    @Schema(description = "优惠券需满金额", example = "100.00")
    @NotNull(message = "满减金额不能为空")
    private BigDecimal achieveAmount;

    /**
     * 优惠券扣减金额【折扣】
     */
    @Schema(description = "优惠券扣减金额或折扣率（折扣类型时范围0-1）", example = "20.00")
    @NotNull(message = "抵扣金额不能为空")
    private BigDecimal deductionAmount;

    /**
     * 领取开始时间
     */
    @Schema(description = "领取开始时间", example = "2025-01-01 00:00:00")
    @NotNull(message = "领取开始时间不能为空")
    private Date receiveStartTime;

    /**
     * 领取结束时间
     */
    @Schema(description = "领取结束时间", example = "2025-12-31 23:59:59")
    @NotNull(message = "领取结束时间不能为空")
    private Date receiveEndTime;

    /**
     * 优惠券领取后有效期计算类型
     * [固定有效期]
     * [根据领取时间计算]
     */
    @Schema(description = "优惠券生效类型：FIXED-固定有效期，AFTER_RECEIVING-根据领取时间计算", example = "FIXED")
    @NotNull(message = "优惠券生效类型不能为空")
    private EffectTimeType effectTimeType;

    /**
     * 优惠券有效开始时间【固定有效期】
     */
    @Schema(description = "优惠券有效开始时间（固定有效期时必填）", example = "2025-01-01 00:00:00")
    private Date useStartTime;

    /**
     * 优惠券有效结束时间【固定有效期】
     */
    @Schema(description = "优惠券有效结束时间（固定有效期时必填）", example = "2025-12-31 23:59:59")
    private Date useEndTime;

    /**
     * 领取多少天后可用【根据领取时间计算】
     */
    @Schema(description = "领取多少天后可用（根据领取时间计算时使用）", example = "0")
    private int receiveDay;

    /**
     * 有效单位【天，分】【根据领取时间计算】
     */
    @Schema(description = "有效单位（根据领取时间计算时使用）", example = "DAY", allowableValues = {"MINUTES", "DAY"})
    private CouponConstant.EffectTimeUnit effectTimeUnit;

    /**
     * 有效期【根据领取时间计算】
     */
    @Schema(description = "有效期数值（根据领取时间计算时使用）", example = "30")
    private int effectNumber;

    /**
     * 发放类型
     */
    @Schema(description = "发放类型", example = "RECEIVE", allowableValues = {"RECEIVE", "ADMIN_ISSUE", "API_ISSUE", "EXPORT_ISSUE"})
    @NotNull(message = "发放类型不能为空")
    private IssueType issueType;

    /**
     * 适用用户范围
     */
    @Schema(description = "适用用户范围", example = "ALL")
    @NotNull(message = "适用用户范围不能为空")
    private ApplyMemberType applyMemberType;

    /**
     * 适用范围
     */
    @Schema(description = "适用范围", example = "ALL", allowableValues = {"ALL", "SPECIFY_PRODUCT", "EXCLUDE_PRODUCT", "SPECIFY_BRAND", "SPECIFY_CATEGORY"})
    @NotNull(message = "适用范围不能为空")
    private ApplyProductRangeType applyProductRangeType;

    /**
     * 适用范围id集合
     */
    @Schema(description = "适用范围ID集合（指定商品/品牌/分类时使用）", example = "[1001, 1002]")
    private List<Long> idList;

    /**
     * 领取限制类型
     */
    @Schema(description = "领取限制类型", example = "ALL_TIME")
    @NotNull(message = "领取限制类型不能为空")
    private LimitReceiveTimeType limitReceiveTimeType;

    /**
     * 领取限制数量
     */
    @Schema(description = "领取限制数量", example = "1", minimum = "1")
    @Min(value = 1, message = "最小数量不能小于1")
    private int limitReceiveNumber;

    /**
     * 优惠券数量
     */
    @Schema(description = "优惠券数量", example = "1000", minimum = "1", maximum = "10000")
    @Min(value = 1, message = "最小数量不能小于1")
    @Max(value = 10000, message = "最大数量不能超过10000")
    private int number;

    /**
     * 活动说明描述
     */
    @Schema(description = "活动说明描述", example = "限新用户使用，满100减20，有效期30天")
    @NotBlank(message = "活动说明不能为空")
    private String desc;

    protected <T extends BaseCouponInfo> void initCouponActivity(T activity) {
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
        activity.setReceiveNumber(0);
        activity.setUseNumber(0);
    }

}
