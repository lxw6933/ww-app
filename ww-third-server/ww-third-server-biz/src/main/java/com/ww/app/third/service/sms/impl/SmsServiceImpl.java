package com.ww.app.third.service.sms.impl;

import com.ww.app.third.service.sms.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/16 15:35
 **/
@Slf4j
@Service
public class SmsServiceImpl implements SmsService {
    @Override
    public boolean sendSms(String mobile, String code) {
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("发送手机号：{} 短信验证码：{}", mobile, code);
        return true;
    }
}
