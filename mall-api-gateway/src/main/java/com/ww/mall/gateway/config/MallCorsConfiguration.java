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

    @Bean
    public CorsWebFilter corsWebFilter(){
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // 配置跨域
        corsConfiguration.addAllowedHeader("*");    // 允许任何请求头
        corsConfiguration.addAllowedOrigin("*");    // 允许任何来源
        corsConfiguration.addAllowedMethod("*");    // 允许任何方式POST GET等
        corsConfiguration.setAllowCredentials(true); // 允许携带cookie

        source.registerCorsConfiguration("/**",corsConfiguration);
        return new CorsWebFilter(source);
    }

}
