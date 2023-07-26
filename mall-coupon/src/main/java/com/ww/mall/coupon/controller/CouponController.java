package com.ww.mall.coupon.controller;

import com.ww.mall.coupon.entity.Coupon;
import com.ww.mall.coupon.service.CouponService;
import com.ww.mall.coupon.view.bo.CouponPageBO;
import com.ww.mall.coupon.view.vo.CouponPageVO;
import com.ww.mall.web.cmmon.MallPage;
import com.ww.mall.web.cmmon.MallPageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author ww
 * @create 2023-07-25- 10:23
 * @description:
 */
@RestController
@RequestMapping("/coupon")
public class CouponController {

    @Autowired
    private CouponService couponService;

    @GetMapping("/activity")
    public MallPageResult<CouponPageVO> pageList(CouponPageBO couponPageBO) {
        return couponService.pageList(couponPageBO);
    }

    @PutMapping("/activity")
    public boolean add(@RequestBody Coupon coupon) {
        return couponService.add(coupon);
    }

    @PutMapping("/activity/{activityCode}")
    public boolean add(@PathVariable("activityCode") String activityCode, @RequestBody Coupon coupon) {
        return couponService.modify(activityCode, coupon);
    }

    @GetMapping("/receiveCoupon")
    public boolean receiveCoupon(@RequestParam("activityCode") String activityCode) {
        return couponService.receiveCoupon(activityCode);
    }

}
