package com.ww.mall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Author:         ww
 * Datetime:       2021\3\5 0005
 * Description:    网关配置跨域
 */
@Configuration
public class MallCorsConfiguration {

    private static final Long MAX_AGE = 3600L;

    @Bean
    public CorsWebFilter corsWebFilter(){
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // 配置跨域
        // 允许任何请求头
        corsConfiguration.addAllowedHeader(CorsConfiguration.ALL);
        // 允许任何来源
        corsConfiguration.addAllowedOrigin(CorsConfiguration.ALL);
        // 允许任何方式POST GET等
        corsConfiguration.addAllowedMethod(CorsConfiguration.ALL);
        // 最大年龄
        corsConfiguration.setMaxAge(MAX_AGE);

        source.registerCorsConfiguration("/**",corsConfiguration);
        return new CorsWebFilter(source);
    }

}
