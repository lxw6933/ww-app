package com.ww.mall.coupon.rpc;

import com.ww.app.common.common.Result;
import com.ww.app.third.sms.fallback.SmsApiFallBack;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author ww
 * @create 2024-11-15- 14:39
 * @description:
 */
@FeignClient(value = "ww-coupon-server", fallbackFactory = SmsApiFallBack.class)
public interface SmsCouponApi {

    @GetMapping("/ww-coupon-server/smsCoupon/receiveCoupon")
    Result<Boolean> receiveCoupon(@RequestParam("activityCode") String activityCode);

    @GetMapping("/ww-coupon-server/smsCoupon/convertCoupon")
    Result<Boolean> convertCoupon(@RequestParam("couponCode") String couponCode);

}
