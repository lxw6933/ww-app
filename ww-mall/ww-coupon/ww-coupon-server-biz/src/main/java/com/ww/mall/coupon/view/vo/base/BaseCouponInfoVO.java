package com.ww.mall.coupon.view.vo.base;

import com.ww.mall.coupon.enums.CouponDiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author ww
 * @create 2025-03-12- 10:49
 * @description:
 */
@Data
@Schema(description = "优惠券基础信息")
public class BaseCouponInfoVO {

    @Schema(description = "活动ID", example = "507f1f77bcf86cd799439011")
    private String id;

    /**
     * 优惠券唯一活动编码
     */
    @Schema(description = "优惠券唯一活动编码", example = "SC1234567890")
    private String activityCode;

    /**
     * 活动名称
     */
    @Schema(description = "活动名称", example = "新用户专享优惠券")
    private String name;

    /**
     * 活动说明描述
     */
    @Schema(description = "活动说明描述", example = "限新用户使用，满100减20")
    private String desc;

    /**
     * 优惠券优惠类型
     */
    @Schema(description = "优惠券优惠类型", example = "DIRECT_REDUCTION")
    private CouponDiscountType couponDiscountType;

    /**
     * 优惠券需满X金额
     */
    @Schema(description = "优惠券需满金额", example = "100.00")
    private BigDecimal achieveAmount;

    /**
     * 优惠券扣减金额【折扣】
     */
    @Schema(description = "优惠券扣减金额或折扣率", example = "20.00")
    private BigDecimal deductionAmount;

}
