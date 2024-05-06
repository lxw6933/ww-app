package com.ww.mall.netty.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author ww
 * @create 2024-05-06 23:06
 * @description:
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "netty")
public class MallNettyProperties {

    private Integer serializerType = 1;
}
