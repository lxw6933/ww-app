package com.ww.mall.third.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author ww
 * @create 2023-03-28- 09:13
 * @description:
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "edu")
public class AppleEduProperties {

    /**
     * edu域名  ip:port
     */
    private String eduDomain;

    /**
     * 请求路劲
     */
    private String reqUri;

    /**
     * 上传图片uri
     */
    private String uploadImgUri;

    /**
     * edu_appId
     */
    private String appId;

    /**
     * edu_security
     */
    private String security;

}
