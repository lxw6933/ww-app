package com.ww.mall.third.service;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/16 15:35
 **/
public interface SmsService {

    /**
     * 发送短信验证码
     *
     * @param mobile 发送手机号
     * @param code 验证码
     * @return boolean
     */
    boolean sendSms(String mobile, String code);

}
