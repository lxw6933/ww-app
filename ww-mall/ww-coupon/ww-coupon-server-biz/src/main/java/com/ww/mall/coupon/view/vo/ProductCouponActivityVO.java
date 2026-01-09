package com.ww.mall.coupon.view.vo;

import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.enums.EffectTimeType;
import com.ww.mall.coupon.view.vo.base.BaseCouponInfoVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author ww
 * @create 2025-03-12- 10:56
 * @description: 商品详情优惠券活动信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商品详情优惠券活动信息")
public class ProductCouponActivityVO extends BaseCouponInfoVO {

    /**
     * 优惠券领取后有效期计算类型
     * [固定有效期]
     * [根据领取时间计算]
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
     * 领取多少天后生效【根据领取时间计算】
     */
    @Schema(description = "领取多少天后生效（根据领取时间计算）", example = "0")
    private int receiveDay;

    /**
     * 有效单位【天，分】【根据领取时间计算】
     */
    @Schema(description = "有效单位（根据领取时间计算）", example = "DAY")
    private CouponConstant.EffectTimeUnit effectTimeUnit;

    /**
     * 多少天的有效期【根据领取时间计算】
     */
    @Schema(description = "有效期数值（根据领取时间计算）", example = "30")
    private int effectNumber;
    
}
