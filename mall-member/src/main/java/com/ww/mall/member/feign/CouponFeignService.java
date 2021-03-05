package com.ww.mall.member.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Author:         ww
 * Datetime:       2021\3\4 0004
 * Description:    调用coupon服务
 */
@FeignClient("mall-coupon")
public interface CouponFeignService {

    @RequestMapping("/coupon/demo")
    public String demo();
}
