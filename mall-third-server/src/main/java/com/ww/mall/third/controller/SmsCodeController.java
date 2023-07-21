package com.ww.mall.third.controller;

import com.ww.mall.third.service.sms.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/16 12:27
 **/
@RestController
@RequestMapping("/sms")
public class SmsCodeController {

    @Autowired
    private SmsService smsService;

    @RequestMapping("/sendCode")
    public boolean sendCode(@RequestParam("mobile") String mobile, @RequestParam("code") String code) {
        return smsService.sendSms(mobile, code);
    }

}
