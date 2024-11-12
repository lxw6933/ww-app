package com.ww.mall.sensitive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author ww
 * @create 2024-05-24 23:41
 * @description:
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "sensitive")
public class MallSensitiveWordProperties {

    private String allowFileUrl = null;

    private String denyFileUrl = null;

}
