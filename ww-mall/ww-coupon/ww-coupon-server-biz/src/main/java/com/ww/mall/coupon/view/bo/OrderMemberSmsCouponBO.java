package com.ww.mall.coupon.view.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author ww
 * @create 2025-03-12- 11:32
 * @description: 确认下单用户平台优惠券请求信息
 */
@Data
@Schema(description = "确认下单商品优惠券请求信息")
public class OrderMemberSmsCouponBO {

    /**
     * 运营商品id【平台优惠券用】
     */
    @Schema(description = "运营商品ID（平台优惠券用）", example = "1001")
    private Long smsId;

    /**
     * 运营商家商品id【商家优惠券用】
     */
    @Schema(description = "运营商家商品ID（商家优惠券用）", example = "2001")
    private Long spuId;

    /**
     * 运营商品规格id【均摊优惠】
     */
    @Schema(description = "运营商品规格ID（均摊优惠用）", example = "3001")
    private Long skuId;

    /**
     * 商品类目id【商家优惠券用】
     */
    @Schema(description = "商品类目ID（商家优惠券用）", example = "4001")
    private Long categoryId;

    /**
     * 商品品牌id【商家优惠券用】
     */
    @Schema(description = "商品品牌ID（商家优惠券用）", example = "5001")
    private Long brandId;

    /**
     * 数量
     */
    @Schema(description = "商品数量", example = "2")
    private Integer number;

    /**
     * 实际支付价格【活动优惠后】
     */
    @Schema(description = "实际支付价格（活动优惠后）", example = "180.00")
    private BigDecimal realAmount;

    /**
     * 实际支付积分【活动优惠后】
     */
    @Schema(description = "实际支付积分（活动优惠后）", example = "1800")
    private Integer realIntegral;

    /**
     * 参与的活动是否允许使用优惠券
     */
    @Schema(description = "参与的活动是否允许使用优惠券", example = "true")
    private boolean activityUseCoupon = true;
}
