package com.ww.mall.coupon.view.vo;

import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.enums.*;
import com.ww.mall.coupon.view.vo.base.BaseCouponInfoVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author ww
 * @create 2023-07-26- 09:16
 * @description: 平台优惠券信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "优惠券活动详情基础信息")
public class BaseCouponDetailVO extends BaseCouponInfoVO {

    /**
     * 开始领取时间
     */
    @Schema(description = "开始领取时间", example = "2025-01-01 00:00:00")
    private Date receiveStartTime;

    /**
     * 结束领取时间
     */
    @Schema(description = "结束领取时间", example = "2025-12-31 23:59:59")
    private Date receiveEndTime;

    /**
     * 优惠券领取后有效期计算类型
     */
    @Schema(description = "优惠券生效类型", example = "FIXED")
    private EffectTimeType effectTimeType;

    /**
     * 优惠券有效开始时间【固定有效期】
     */
    @Schema(description = "优惠券有效开始时间（固定有效期）", example = "2025-01-01 00:00:00")
    private Date useStartTime;

    /**
     * 优惠券有效结束时间【固定有效期】
     */
    @Schema(description = "优惠券有效结束时间（固定有效期）", example = "2025-12-31 23:59:59")
    private Date useEndTime;

    /**
     * 领取多少天后可用【根据领取时间计算】
     */
    @Schema(description = "领取多少天后可用（根据领取时间计算）", example = "0")
    private int receiveDay;

    /**
     * 有效单位【天，分】【根据领取时间计算】
     */
    @Schema(description = "有效单位（根据领取时间计算）", example = "DAY")
    private CouponConstant.EffectTimeUnit effectTimeUnit;

    /**
     * 有效期【根据领取时间计算】
     */
    @Schema(description = "有效期数值（根据领取时间计算）", example = "30")
    private int effectNumber;

    /**
     * 发放类型
     */
    @Schema(description = "发放类型", example = "RECEIVE")
    private IssueType issueType;

    /**
     * 适用用户范围
     */
    @Schema(description = "适用用户范围", example = "ALL")
    private ApplyMemberType applyMemberType;

    /**
     * 适用范围
     */
    @Schema(description = "适用范围", example = "ALL")
    private ApplyProductRangeType applyProductRangeType;

    /**
     * 适用范围id集合
     */
    @Schema(description = "适用范围ID集合", example = "[1001, 1002]")
    private List<Long> idList;

    /**
     * 领取限制类型
     */
    @Schema(description = "领取限制类型", example = "ALL_TIME")
    private LimitReceiveTimeType limitReceiveTimeType;

    /**
     * 领取限制数量
     */
    @Schema(description = "领取限制数量", example = "1")
    private int limitReceiveNumber;

    /**
     * 优惠券数量
     */
    @Schema(description = "优惠券数量", example = "1000")
    private Integer number;

}
