package com.ww.app.third.rpc;

import com.ww.app.common.common.Result;
import com.ww.app.third.service.sms.SmsService;
import com.ww.app.third.sms.rpc.SmsApi;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/16 12:27
 **/
@RestController
@RequestMapping("/sms")
public class SmsApiRpc implements SmsApi {

    @Resource
    private SmsService smsService;

    @Override
    @GetMapping("/sendCode")
    public Result<Boolean> sendSms(String mobile, String code) {
        return Result.success(smsService.sendSms(mobile, code));
    }
}
