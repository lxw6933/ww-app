package com.ww.mall.im.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author ww
 * @create 2024-11-09 21:24
 * @description:
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "im")
public class ImProperties {

    /**
     * 默认序列化类型
     */
    private Integer serializerType = 1;

    /**
     * im server 端口号
     */
    private int port = 8765;

    private int disconnectClientTime = 15;

    private int clientHeartbeatTime = 5;

}
