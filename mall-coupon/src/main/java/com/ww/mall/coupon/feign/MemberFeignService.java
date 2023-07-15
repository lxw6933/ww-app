package com.ww.mall.coupon.feign;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/15 20:53
 **/
@FeignClient("mall-member")
public interface MemberFeignService {

}
