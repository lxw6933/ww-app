package com.ww.app.coupon.controller;

import com.ww.app.coupon.entity.Coupon;
import com.ww.app.coupon.service.CouponService;
import com.ww.app.coupon.view.bo.CouponPageBO;
import com.ww.app.coupon.view.vo.CouponPageVO;
import com.ww.app.common.common.AppPageResult;
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
    public AppPageResult<CouponPageVO> pageList(CouponPageBO couponPageBO) {
        return couponService.pageList(couponPageBO);
    }

    @PutMapping("/activity")
    public boolean add(@RequestBody Coupon coupon) {
        return couponService.add(coupon);
    }

    @PutMapping("/activity/{activityCode}")
    public boolean modify(@PathVariable("activityCode") String activityCode, @RequestBody Coupon coupon) {
        return couponService.modify(activityCode, coupon);
    }

    @GetMapping("/receiveCoupon")
    public boolean receiveCoupon(@RequestParam("activityCode") String activityCode) {
        return couponService.receiveCoupon(activityCode);
    }

}
