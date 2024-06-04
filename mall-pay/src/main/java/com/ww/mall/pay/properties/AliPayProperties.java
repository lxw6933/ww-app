package com.ww.mall.pay.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author ww
 * @create 2024-06-04- 18:56
 * @description:
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AliPayProperties {

    private String appId;

    private String privateKey;

    private String publicKey;

    private String appCertPath;

    private String aliPayCertPath;

    private String aliPayRootCertPath;

    private String serverUrl;

    private String domain;

}
