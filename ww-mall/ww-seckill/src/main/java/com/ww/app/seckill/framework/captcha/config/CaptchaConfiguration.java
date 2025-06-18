package com.ww.app.seckill.framework.captcha.config;

import com.anji.captcha.config.AjCaptchaAutoConfiguration;
import com.anji.captcha.properties.AjCaptchaProperties;
import com.anji.captcha.service.CaptchaCacheService;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import com.ww.app.seckill.framework.captcha.core.RedisCaptchaServiceImpl;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
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
// 目的：解决 aj-captcha 针对 SpringBoot 3.X 自动配置不生效的问题
@ImportAutoConfiguration(AjCaptchaAutoConfiguration.class)
public class CaptchaConfiguration {

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
