package com.ww.mall.netty.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author ww
 * @create 2024-05-06 23:06
 * @description:
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ws")
public class MallWSProperties {

    private Integer serializerType = 2;

    private Integer serverPort = 8765;

    private Integer websocketPort = 8764;

    private String websocketPath = "/ws";

    private Integer disconnectClientTime = 15;

    private Integer clientHeartbeatTime = 5;
}
