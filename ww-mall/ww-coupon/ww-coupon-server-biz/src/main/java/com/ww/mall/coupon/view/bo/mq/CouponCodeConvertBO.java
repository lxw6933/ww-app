package com.ww.mall.coupon.view.bo.mq;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author ww
 * @create 2025-03-20- 20:30
 * @description:
 */
@Data
@AllArgsConstructor
public class CouponCodeConvertBO {

    private Long userId;

    private Long channelId;

    private String couponCode;

}
