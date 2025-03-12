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
     * 运营商品id
     */
    private Long smsId;

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
}
