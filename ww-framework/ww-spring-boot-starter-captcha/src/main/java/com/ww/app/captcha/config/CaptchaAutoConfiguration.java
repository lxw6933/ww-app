package com.ww.app.captcha.config;

import com.anji.captcha.properties.AjCaptchaProperties;
import com.anji.captcha.service.CaptchaCacheService;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import com.ww.app.captcha.core.anji.RedisCaptchaServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author ww
 * @create 2025-06-18- 17:11
 * @description:
 */
@Configuration(proxyBeanMethods = false)
public class CaptchaAutoConfiguration {

    @Primary
    @Bean(name = "AjCaptchaCacheService")
    public CaptchaCacheService captchaCacheService(AjCaptchaProperties config, StringRedisTemplate stringRedisTemplate) {
        CaptchaCacheService captchaCacheService = CaptchaServiceFactory.getCache(config.getCacheType().name());
        if (captchaCacheService instanceof RedisCaptchaServiceImpl) {
            ((RedisCaptchaServiceImpl) captchaCacheService).setStringRedisTemplate(stringRedisTemplate);
        }
        return captchaCacheService;
    }

}
