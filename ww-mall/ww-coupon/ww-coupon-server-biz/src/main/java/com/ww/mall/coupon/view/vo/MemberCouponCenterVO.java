package com.ww.mall.coupon.view.vo;

import com.ww.mall.coupon.enums.ApplyProductRangeType;
import com.ww.mall.coupon.enums.CouponStatus;
import com.ww.mall.coupon.enums.CouponType;
import com.ww.mall.coupon.view.vo.base.BaseCouponInfoVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author ww
 * @create 2025-03-11- 13:57
 * @description: 用户个人中心优惠券中心信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "会员卡券中心优惠券信息")
public class MemberCouponCenterVO extends BaseCouponInfoVO {

    /**
     * 优惠券类型【店铺、渠道】
     */
    @Schema(description = "优惠券类型：PLATFORM-平台券，MERCHANT-商家券", example = "PLATFORM")
    private CouponType couponType;

    /**
     * 商家id【商家券用】
     */
    @Schema(description = "商家ID（商家优惠券用）", example = "1001")
    private Long merchantId;

    /**
     * 开始使用时间
     */
    @Schema(description = "开始使用时间", example = "2025-01-01 00:00:00")
    private Date useStartTime;

    /**
     * 过期时间
     */
    @Schema(description = "过期时间", example = "2025-12-31 23:59:59")
    private Date useEndTime;

    /**
     * 优惠券券码状态
     */
    @Schema(description = "优惠券券码状态", example = "IN_EFFECT")
    private CouponStatus couponStatus;

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
    
}
