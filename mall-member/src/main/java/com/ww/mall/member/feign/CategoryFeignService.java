package com.ww.mall.member.feign;

import com.ww.mall.common.common.R;
import com.ww.mall.member.to.CategoryTo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @Author:         ww
 * @Datetime:       2021\3\4 0004
 * @Description:    调用coupon服务
 */
@FeignClient("mall-product")
public interface CategoryFeignService {

    @PostMapping("/product/category/get")
    R list(@RequestBody CategoryTo categoryTO);

}
