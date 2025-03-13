package com.ww.mall.coupon.rpc;

import com.ww.app.common.common.Result;
import com.ww.app.common.context.AuthorizationContext;
import com.ww.mall.coupon.service.SmsCouponService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2025-03-06- 17:30
 * @description:
 */
@RestController
@RequestMapping("/smsCoupon")
public class SmsCouponRpc implements SmsCouponApi {

    @Resource
    private SmsCouponService smsCouponService;

    @Override
    public Result<Boolean> receiveCoupon(String activityCode) {
        return Result.success(smsCouponService.receiveCoupon(AuthorizationContext.getClientUser(), activityCode));
    }

    @Override
    public Result<Boolean> convertCoupon(String couponCode) {
        return Result.success(smsCouponService.convertCoupon(couponCode));
    }
}
