package com.ww.app.auth.serivce.impl;

import cn.hutool.core.util.IdUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wf.captcha.SpecCaptcha;
import com.ww.app.auth.serivce.ICaptchaService;
import com.ww.app.auth.view.vo.CaptchaResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * @author NineSu
 */
@Slf4j
@Service
public class CaptchaService implements ICaptchaService {

    private final Duration CAPTCHA_EXPIRE = Duration.ofMinutes(2);
    private final Cache<String, String> capctchaCache = Caffeine.newBuilder()
            .expireAfterWrite(CAPTCHA_EXPIRE)
            .build();

    @Override
    public CaptchaResp image(int width, int height) {
        // 定义图形验证码的长和宽
        // png类型
        SpecCaptcha captcha = new SpecCaptcha(width, height, 4);
        String uuid = IdUtil.fastUUID();
        String code = captcha.text(); // 获取验证码的字符
        String imageBase64Data = captcha.toBase64();
        capctchaCache.put(uuid, code);
        return new CaptchaResp(uuid, imageBase64Data, CAPTCHA_EXPIRE.getSeconds());
    }

    @Override
    public boolean validate(String uuid, String value) {
        if (StringUtils.isAnyBlank(uuid, value)) {
            return false;
        }
        String code = capctchaCache.getIfPresent(uuid);
        if (code != null && code.equalsIgnoreCase(value)) {
            capctchaCache.invalidate(uuid);
            return true;
        }
        return false;
    }
}
