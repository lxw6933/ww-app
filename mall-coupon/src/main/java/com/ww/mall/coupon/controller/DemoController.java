package com.ww.mall.coupon.controller;

import com.ww.mall.common.utils.SecretUtils;
import com.ww.mall.coupon.config.CouponProperties;
import com.ww.mall.web.config.SecretProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Author:         ww
 * Datetime:       2021\3\4 0004
 * Description:
 */
@RestController
@RequestMapping("/coupon")
public class DemoController {

    @Autowired
    private CouponProperties couponProperties;

    @Autowired
    private SecretProperties secretProperties;

    @RequestMapping("/demo")
    public String demo(){
        return "coupon active is opening！！！" + secretProperties;
    }

    @RequestMapping("/test")
    public String test(){
        int i = 1 / 0;
        return "coupon test";
    }


}

