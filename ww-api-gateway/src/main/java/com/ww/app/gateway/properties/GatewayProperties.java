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
public class GatewayProperties {

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

}
