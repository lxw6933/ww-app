package com.ww.mall.member.controller;

import com.ww.mall.common.constant.R;
import com.ww.mall.member.feign.CategoryFeignService;
import com.ww.mall.member.feign.CouponFeignService;
import com.ww.mall.member.to.CategoryTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.PostMapping;
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
    @Autowired
    private CategoryFeignService categoryFeignService;

    @RequestMapping("/get/coupon")
    public String getCouponInfo(){
        String couponInfo = couponFeignService.demo();
        return name+"查询到的优惠券信息："+couponInfo;
    }

    @PostMapping("/get/category")
    public String getCategoryInfo(){
        CategoryTO categoryTO = new CategoryTO(1);
        R list = categoryFeignService.list(categoryTO);
        return name+"查询到的category信息："+list;
    }


    @RequestMapping("/test")
    public String test(){
        return "member test";
    }

}

