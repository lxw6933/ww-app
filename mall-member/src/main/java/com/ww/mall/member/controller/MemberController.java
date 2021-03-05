package com.ww.mall.member.controller;

import com.ww.mall.member.feign.CouponFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Author:         ww
 * Datetime:       2021\3\4 0004
 * Description:
 */
@RefreshScope
@RestController
@RequestMapping("/member")
public class MemberController {

    @Value("${member.name}")
    private String name;

    @Autowired
    private CouponFeignService couponFeignService;

    @RequestMapping("/get/coupon")
    public String getCouponInfo(){
        String couponInfo = couponFeignService.demo();
        return name+"查询到的优惠券信息："+couponInfo;
    }

    @RequestMapping("/test")
    public String test(){
        return "member test";
    }

}

