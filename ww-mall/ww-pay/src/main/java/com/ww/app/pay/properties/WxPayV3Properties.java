package com.ww.app.pay.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author ww
 * @create 2024-06-05- 14:27
 * @description:
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wxpay.v3")
public class WxPayV3Properties {

    private String appId;

    private String keyPath;

    private String certPath;

    private String certP12Path;

    private String platformCertPath;

    private String mchId;

    private String apiKey3;

    private String apiKey;

    private String domain;

}
