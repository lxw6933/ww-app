package com.ww.mall.coupon.controller;

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

    @RequestMapping("/demo")
    public String demo(){
        return "coupon active is opening！！！";
    }

    @RequestMapping("/test")
    public String test(){
        int i = 1 / 0;
        return "coupon test";
    }


}
