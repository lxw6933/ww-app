package com.ww.mall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * @description:
 * @author: ww
 * @create: 2021-06-15 11:28
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 注册RestTemplate
     *
     * @param factory 工厂
     * @return RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(ClientHttpRequestFactory factory) {
        return new RestTemplate(factory);
    }

    /**
     * 请求工厂
     *
     * @return ClientHttpRequestFactory
     */
    @Bean
    public ClientHttpRequestFactory simpleClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 连接超时时间
        factory.setConnectTimeout(15000);
        // 信息读取超时时间
        factory.setReadTimeout(5000);
        return factory;
    }

}
