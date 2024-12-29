package com.ww.mall.coupon.view.bo;

import com.ww.mall.coupon.eunms.CouponDiscountType;
import com.ww.mall.coupon.eunms.CouponType;
import com.ww.mall.common.common.AppPage;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ww
 * @create 2023-07-26- 09:25
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CouponPageBO extends AppPage {

    /**
     * 优惠券名称
     */
    private String title;

    /**
     * 优惠券类型
     */
    private CouponType couponType;

    /**
     * 优惠券优惠类型
     */
    private CouponDiscountType couponDiscountType;

}
