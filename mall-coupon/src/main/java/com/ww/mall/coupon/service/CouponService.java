package com.ww.mall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ww.mall.coupon.entity.Coupon;

/**
 * @author ww
 * @create 2023-07-25- 10:19
 * @description:
 */
public interface CouponService extends IService<Coupon> {

    /**
     * 新增优惠券活动
     *
     * @param coupon coupon
     * @return boolean
     */
    boolean add(Coupon coupon);

    /**
     * 修改优惠券活动
     *
     * @param coupon coupon
     * @param activityCode 优惠券编码
     * @return boolean
     */
    boolean modify(String activityCode, Coupon coupon);

    /**
     * 用户领取优惠券
     *
     * @param activityCode 优惠券编码
     * @return boolean
     */
    boolean receiveCoupon(String activityCode);

}
