package com.ww.app.gateway.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author ww
 * @create 2023-08-04- 18:09
 * @description:
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ww")
public class AppGatewayProperties {

    /**
     * 不需要用户登录信息的uri集合
     */
    private List<String> whiteUriList;

    /**
     * 不需要加密响应结果的uri集合
     */
    private List<String> decryptUriList;

    /**
     * ip黑名单
     */
    private List<String> blackIpList;

    /**
     * 是否启用访问日志
     */
    private Boolean accessLogEnabled = true;

    /**
     * 访问日志采样率（0-1）
     */
    private Double accessLogSampleRate = 0.1d;

    /**
     * 访问日志记录的最大请求/响应体大小（字节）
     */
    private Integer accessLogMaxBodySizeBytes = 16 * 1024;

    /**
     * 是否记录请求体
     */
    private Boolean accessLogLogRequestBody = true;

    /**
     * 是否记录响应体
     */
    private Boolean accessLogLogResponseBody = true;

    /**
     * 是否启用JWT缓存
     */
    private Boolean jwtCacheEnabled = true;

    /**
     * JWT缓存最大条数
     */
    private Integer jwtCacheMaxSize = 10000;

    /**
     * JWT过期时间提前失效秒数，降低时钟偏差风险
     */
    private Integer jwtCacheSkewSeconds = 5;

}
