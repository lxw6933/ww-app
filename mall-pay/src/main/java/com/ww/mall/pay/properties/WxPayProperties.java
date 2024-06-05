package com.ww.mall.pay.properties;

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
@ConfigurationProperties(prefix = "wxpay")
public class WxPayProperties {

    private String appId;

    private String appSecret;

    private String mchId;

    private String partnerKey;

    private String certPath;

    private String domain;

}
