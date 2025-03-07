package com.ww.mall.coupon.controller;

import com.ww.app.common.common.AppPageResult;
import com.ww.mall.coupon.service.SmsCouponService;
import com.ww.mall.coupon.view.bo.SmsCouponActivityAddBO;
import com.ww.mall.coupon.view.bo.SmsCouponCodeListBO;
import com.ww.mall.coupon.view.bo.SmsCouponPageBO;
import com.ww.mall.coupon.view.vo.SmsCouponCodeListVO;
import com.ww.mall.coupon.view.vo.SmsCouponPageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author ww
 * @create 2023-07-25- 10:23
 * @description:
 */
@RestController
@RequestMapping("/coupon")
public class SmsCouponController {

    @Autowired
    private SmsCouponService smsCouponService;

    @GetMapping("/activity")
    public AppPageResult<SmsCouponPageVO> pageList(SmsCouponPageBO smsCouponPageBO) {
        return smsCouponService.pageList(smsCouponPageBO);
    }

    @GetMapping("/activity/codes")
    public List<SmsCouponCodeListVO> codeList(SmsCouponCodeListBO smsCouponCodeListBO) {
        return smsCouponService.codeList(smsCouponCodeListBO);
    }

    @PutMapping("/activity")
    public boolean add(@RequestBody SmsCouponActivityAddBO smsCouponActivityAddBO) {
        return smsCouponService.add(smsCouponActivityAddBO);
    }

    @GetMapping("/receiveCoupon")
    public boolean receiveCoupon(@RequestParam("activityCode") String activityCode) {
        return smsCouponService.receiveCoupon(activityCode);
    }

    @GetMapping("/convertCoupon")
    public boolean convertCoupon(@RequestParam("couponCode") String couponCode) {
        return smsCouponService.convertCoupon(couponCode);
    }

}
