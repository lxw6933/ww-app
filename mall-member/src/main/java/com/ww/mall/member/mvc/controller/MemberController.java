package com.ww.mall.member.mvc.controller;

import com.ww.mall.common.common.R;
import com.ww.mall.member.config.mybatisplus.page.Pagination;
import com.ww.mall.member.feign.AdminFeignService;
import com.ww.mall.member.feign.CategoryFeignService;
import com.ww.mall.member.feign.CouponFeignService;
import com.ww.mall.member.to.AdminUserTo;
import com.ww.mall.member.to.CategoryTo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Author:         ww
 * @Datetime:       2021\3\4 0004
 * @Description:
 */
@RefreshScope
@RestController
@RequestMapping("/member")
public class MemberController {

    @Value("${member.name}")
    private String name;

    @Resource
    private CouponFeignService couponFeignService;
    @Resource
    private CategoryFeignService categoryFeignService;
    @Resource
    private AdminFeignService adminFeignService;

    @GetMapping("/testConfig")
    public String testConfig() {
        return name;
    }

    @RequestMapping("/get/coupon")
    public String getCouponInfo(){
        String couponInfo = couponFeignService.demo();
        return name+"查询到的优惠券信息："+couponInfo;
    }

    @PostMapping("/get/category")
    public String getCategoryInfo(){
        CategoryTo categoryTo = new CategoryTo(1);
        R list = categoryFeignService.list(categoryTo);
        return name+"查询到的category信息："+list;
    }


    @RequestMapping("/test/admin")
    public String test(Pagination pagination, AdminUserTo query){
        R page = adminFeignService.page(pagination, query);
        return page.toString();
    }

}

