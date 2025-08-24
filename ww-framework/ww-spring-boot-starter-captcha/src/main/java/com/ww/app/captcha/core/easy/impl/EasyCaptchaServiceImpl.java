package com.ww.app.captcha.core.easy.impl;

import cn.hutool.core.util.IdUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.wf.captcha.SpecCaptcha;
import com.ww.app.captcha.core.easy.EasyCaptchaService;
import com.ww.app.captcha.core.easy.vo.EasyCaptchaVO;
import com.ww.app.common.utils.CaffeineUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2025-08-24 9:55
 * @description:
 */
@Slf4j
public class EasyCaptchaServiceImpl implements EasyCaptchaService {

    private final int EXPIRE_TIME = 120;
    private final Cache<String, String> captchaCache = CaffeineUtil.createCache(100, 500, EXPIRE_TIME, TimeUnit.SECONDS);

    @Override
    public EasyCaptchaVO image(int width, int height) {
        // 定义图形验证码的长和宽
        // png类型
        SpecCaptcha captcha = new SpecCaptcha(width, height, 4);
        String uuid = IdUtil.fastUUID();
        String code = captcha.text();
        String imageBase64Data = captcha.toBase64();
        captchaCache.put(uuid, code);
        return new EasyCaptchaVO(uuid, imageBase64Data, (long) EXPIRE_TIME);
    }

    @Override
    public boolean validate(String uuid, String value) {
        if (StringUtils.isAnyBlank(uuid, value)) {
            return false;
        }
        String code = captchaCache.getIfPresent(uuid);
        if (code != null && code.equalsIgnoreCase(value)) {
            captchaCache.invalidate(uuid);
            return true;
        }
        return false;
    }

}
