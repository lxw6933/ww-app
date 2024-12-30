package com.ww.app.third.sms.rpc;

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
@FeignClient(value = "ww-third-server", fallbackFactory = SmsApiFallBack.class)
public interface SmsApi {

    /**
     * 发送短信验证码
     *
     * @param mobile 发送手机号
     * @param code 验证码
     * @return boolean
     */
    @GetMapping("/ww-third-server/sms/sendCode")
    Result<Boolean> sendSms(@RequestParam("mobile") String mobile, @RequestParam("code") String code);

}
