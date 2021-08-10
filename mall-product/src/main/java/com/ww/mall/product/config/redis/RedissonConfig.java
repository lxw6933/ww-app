package com.ww.mall.product.config.redis;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description: redission 配置类
 * @author: ww
 * @create: 2021/5/15 下午1:54
 **/
@Slf4j
@Configuration
public class RedissonConfig {

    /**
     * "redis://"正常连接
     */
    private static final String REDISSON_PREFIX = "redis://";

    /**
     * "rediss://"来启用SSL连接
     */
    private static final String REDISSON_SSL_PREFIX = "rediss://";

    @Value("${spring.redis.port}")
    private String port;

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.password}")
    private String password;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String url = REDISSON_PREFIX + host + ":" + port;
        // 这里以单台redis服务器为例
        config.useSingleServer()
                .setAddress(url)
                .setPassword(password);
        // 集群配置
//        String[] urls = {"127.0.0.1:6379", "127.0.0.2:6379"};
//        config.useClusterServers()
//                .addNodeAddress(urls);
        // 主从配置
//        config.useMasterSlaveServers()
//                .setMasterAddress("")
//                .setPassword("")
//                .addSlaveAddress(urls);

        try {
            return Redisson.create(config);
        } catch (Exception e) {
            log.error("RedissonClient init redis url:[{}], Exception:", url, e);
            return null;
        }
    }

}
