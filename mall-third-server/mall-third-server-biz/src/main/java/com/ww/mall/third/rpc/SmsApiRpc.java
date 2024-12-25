package com.ww.mall.third.rpc;

import com.ww.mall.common.common.Result;
import com.ww.mall.third.service.sms.SmsService;
import com.ww.mall.third.sms.rpc.SmsApi;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/16 12:27
 **/
@RestController
public class SmsApiRpc implements SmsApi {

    @Resource
    private SmsService smsService;

    @Override
    public Result<Boolean> sendSms(String mobile, String code) {
        return Result.success(smsService.sendSms(mobile, code));
    }
}
