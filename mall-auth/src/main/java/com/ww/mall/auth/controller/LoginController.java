package com.ww.mall.auth.controller;

import cn.hutool.core.util.RandomUtil;
import com.ww.mall.auth.feign.ThirdServerFeignService;
import com.ww.mall.common.common.Result;
import com.ww.mall.common.constant.Constant;
import com.ww.mall.common.enums.CodeEnum;
import com.ww.mall.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Pattern;
import java.util.concurrent.TimeUnit;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/16 12:40
 **/
@Slf4j
@Validated
@RestController
@RequestMapping("/login")
public class LoginController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ThirdServerFeignService thirdServerFeignService;

    @GetMapping("/sendCode")
    public void sendCode(@Pattern(regexp = "^1[3456789]\\d{9}$", message = "请输入正确的手机号码") @RequestParam("mobile") String mobile) {
        String mobileCode = stringRedisTemplate.opsForValue().get(Constant.SMS_CODE_CACHE_PREFIX + mobile);
        if (StringUtils.isNotEmpty(mobileCode)) {
            // 判断是否超过验证码过期时间
            long mobileCodeTime = Long.parseLong(mobileCode.split("_")[1]);
            if (System.currentTimeMillis() - mobileCodeTime < 60000) {
                // 验证码一分钟内不能重发
                throw new ApiException(CodeEnum.SMS_CODE_EXCEPTION.getCode(), CodeEnum.SMS_CODE_EXCEPTION.getMessage());
            }
        }
        // 生成新的验证码
        String newCode = RandomUtil.randomNumbers(6);
        // 记录验证码生成的时间
        String newCodeTime =  newCode + "_" + System.currentTimeMillis();
        // 验证码三分钟内有效
        stringRedisTemplate.opsForValue()
                .set(Constant.SMS_CODE_CACHE_PREFIX + mobile, newCodeTime, 3, TimeUnit.MINUTES);
        // 发送验证码短信
        Result<Boolean> sendSmsResult = thirdServerFeignService.sendSms(mobile, newCode);
        if (Boolean.TRUE.equals(sendSmsResult.isSuccess())) {
            if (Boolean.TRUE.equals(sendSmsResult.getValue())) {
                log.info("发送短信验证码成功");
            } else {
                throw new ApiException("发送短信验证码失败");
            }
        } else {
            throw new ApiException("调用发送短信接口失败");
        }
    }

}
