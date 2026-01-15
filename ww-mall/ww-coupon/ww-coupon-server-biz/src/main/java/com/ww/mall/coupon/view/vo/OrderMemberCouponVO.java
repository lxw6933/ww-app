package com.ww.mall.coupon.view.vo;

import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.enums.CouponType;
import com.ww.mall.coupon.view.vo.base.BaseCouponInfoVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * @author ww
 * @create 2025-03-12- 11:09
 * @description: 确认下单用户优惠券vo
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "确认下单用户优惠券信息")
public class OrderMemberCouponVO extends BaseCouponInfoVO {

    /**
     * 优惠券类型
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
     * 最高优惠总金额/积分
     */
    @Schema(description = "最高优惠总金额或积分", example = "20.00")
    private BigDecimal discountTotalAmount = BigDecimal.ZERO;

    /**
     * 还差多少金额或者积分可用优惠券
     */
    @Schema(description = "还差多少金额或积分可用优惠券", example = "50.00")
    private BigDecimal lackAmount = BigDecimal.ZERO;

    /**
     * 不可用原因
     */
    @Schema(description = "不可用原因", example = "UN_REACHED_TIME", allowableValues = {"UN_REACHED_TIME", "NO_PRODUCT", "DISCOUNT_ZERO"})
    private CouponConstant.Disabled disabled;

    /**
     * 商品均摊金额
     */
    @Schema(description = "商品均摊金额（SKU ID -> 均摊金额）", example = "{\"1001\": 10.00, \"1002\": 10.00}")
    private Map<Long, BigDecimal> allocateResultMap;
}
