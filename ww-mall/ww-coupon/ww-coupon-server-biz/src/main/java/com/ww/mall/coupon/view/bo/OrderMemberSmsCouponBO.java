package com.ww.mall.coupon.view.bo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author ww
 * @create 2025-03-12- 11:32
 * @description: 确认下单用户平台优惠券请求信息
 */
@Data
public class OrderMemberSmsCouponBO {

    /**
     * 运营商品id【平台优惠券用】
     */
    private Long smsId;

    /**
     * 运营商家商品id【商家优惠券用】
     */
    private Long spuId;

    /**
     * 运营商品规格id【均摊优惠】
     */
    private Long skuId;

    /**
     * 商品类目id【商家优惠券用】
     */
    private Long categoryId;

    /**
     * 商品品牌id【商家优惠券用】
     */
    private Long brandId;

    /**
     * 数量
     */
    private Integer number;

    /**
     * 实际支付价格【活动优惠后】
     */
    private BigDecimal realAmount;

    /**
     * 实际支付积分【活动优惠后】
     */
    private Integer realIntegral;

    /**
     * 参与的活动是否允许使用优惠券
     */
    private boolean activityUseCoupon = true;
}
