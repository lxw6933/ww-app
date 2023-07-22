package com.ww.mall.web.feign;

import com.ww.mall.common.common.Result;
import com.ww.mall.web.feign.inner.ThirdServerFeignServiceFallBack;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/16 15:44
 **/
@FeignClient(value = "mall-third-server", fallbackFactory = ThirdServerFeignServiceFallBack.class)
public interface ThirdServerFeignService {

    /**
     * 发送短信验证码
     *
     * @param mobile 发送手机号
     * @param code 验证码
     * @return boolean
     */
    @RequestMapping("/mall-third-server/sms/sendCode")
    Result<Boolean> sendSms(@RequestParam("mobile") String mobile, @RequestParam("code") String code);

}
