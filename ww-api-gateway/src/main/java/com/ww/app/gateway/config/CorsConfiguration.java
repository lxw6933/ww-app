package com.ww.app.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Author:         ww
 * Datetime:       2021\3\5 0005
 * Description:    网关配置跨域
 */
@Configuration
public class CorsConfiguration {

    private static final Long MAX_AGE = 3600L;

    @Bean
    public CorsWebFilter corsWebFilter(){
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        org.springframework.web.cors.CorsConfiguration corsConfiguration = new org.springframework.web.cors.CorsConfiguration();
        // 配置跨域
        // 允许任何请求头
        corsConfiguration.addAllowedHeader(org.springframework.web.cors.CorsConfiguration.ALL);
        // 允许任何来源
        corsConfiguration.addAllowedOriginPattern(org.springframework.web.cors.CorsConfiguration.ALL);
        // 允许任何方式POST GET等
        corsConfiguration.addAllowedMethod(org.springframework.web.cors.CorsConfiguration.ALL);
        // 允许携带cookie
        corsConfiguration.setAllowCredentials(true);
        // 最大年龄
        corsConfiguration.setMaxAge(MAX_AGE);

        source.registerCorsConfiguration("/**",corsConfiguration);
        return new CorsWebFilter(source);
    }

}
