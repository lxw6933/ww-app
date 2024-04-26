package com.ww.mall.gateway.properties;

import com.ww.mall.gateway.utils.GrayLoadBalancer;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author ww
 * @create 2023-07-31- 17:48
 * @description:
 */
@Data
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "server.gray")
public class ServerGrayProperties {

    /**
     * 生产的版本
     */
    private String proVersion;

    /**
     * 需要灰度的人员列表
     */
    private List<String> grayUsers;

    /**
     * 灰度ip列表
     */
    private List<String> grayIps;

    /**
     * 灰度的版本
     */
    private String grayVersion;

    /**
     * 权重
     */
    private Double weight;

    /**
     * 是否开启{@link GrayLoadBalancer} 的方式进行灰度发布
     */
    private Boolean enable = true;
}


