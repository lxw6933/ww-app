package com.ww.mall.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * @author ww
 * @create 2023-07-15- 15:53
 * @description:
 */
@Data
@ConfigurationProperties(prefix = "redis-cache-manager")
public class MallRedisCacheProperties {

    private Boolean cacheNullValues = Boolean.FALSE;

    private Duration defaultExpiration = Duration.ofSeconds(60L);

    private Map<String, Duration> expires;

}
