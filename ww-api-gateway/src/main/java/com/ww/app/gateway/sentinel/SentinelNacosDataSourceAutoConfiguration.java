package com.ww.app.gateway.sentinel;

import com.alibaba.cloud.sentinel.SentinelProperties;
import com.alibaba.cloud.sentinel.custom.SentinelAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ww
 * @create 2023-07-20- 11:22
 * @description: sentinel dashboard规则持久化到 nacos 配置
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(SentinelAutoConfiguration.class)
public class SentinelNacosDataSourceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SentinelNacosDataSourceHandler sentinelNacosDataSourceHandler(SentinelProperties sentinelProperties) {
        return new SentinelNacosDataSourceHandler(sentinelProperties);
    }

}
