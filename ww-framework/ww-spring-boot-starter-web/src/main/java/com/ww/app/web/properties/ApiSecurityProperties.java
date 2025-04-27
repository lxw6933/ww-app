package com.ww.app.web.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * API安全配置属性
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "ww.security.api")
public class ApiSecurityProperties {

    /**
     * 是否启用API安全验证
     */
    private boolean enabled = true;

    /**
     * 签名请求头名称
     */
    private String signHeaderName = "X-Sign";

    /**
     * 时间戳请求头名称
     */
    private String timestampHeaderName = "X-Timestamp";

    /**
     * API密钥配置
     * key: 客户端标识（appId）
     * value: 密钥（secretKey）
     */
    private Map<String, String> secrets = new HashMap<>();

    /**
     * 应用ID请求头名称
     */
    private String appIdHeaderName = "X-App-Id";

    /**
     * 时间戳有效期(秒)
     */
    private long timestampExpire = 60;

    /**
     * 不需要验证的URL路径
     */
    private String[] excludePaths = {};

}