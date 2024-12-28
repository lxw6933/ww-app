package com.ww.mall.redis.config;

import com.ww.mall.redis.aspect.DistributedLockAspect;
import com.ww.mall.redis.codec.RedissonCodec;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author ww
 * @create 2023-07-15- 17:22
 * @description:
 */
@Slf4j
@ConditionalOnClass({RedisTemplate.class})
@EnableConfigurationProperties(RedisProperties.class)
public class RedissonAutoConfig {

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

//    @Value("${spring.redis.cluster.nodes}")
//    private String redisNodes;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        // 这里以单台redis服务器为例
        String url = REDISSON_PREFIX + host + ":" + port;
        config.useSingleServer()
                .setAddress(url)
                .setPassword(password);
        // 序列化
        config.setCodec(new RedissonCodec());
        // 集群配置
//        String[] urls = redisNodes.split(",");
//        String[] nodes = new String[urls.length];
//        for (int i = 0; i < urls.length; i++) {
//            nodes[i] =  REDISSON_PREFIX + urls[i];
//        }
//        config.useClusterServers()
//                .addNodeAddress(nodes);
        // 主从配置
//        config.useMasterSlaveServers()
//                .setMasterAddress("")
//                .setPassword("")
//                .addSlaveAddress(urls);

        try {
            log.info("初始化redission成功...");
            return Redisson.create(config);
        } catch (Exception e) {
            log.error("RedissonClient init redis url:[{}], Exception:", url, e);
//            log.error("RedissonClient init redis url:[{}], Exception:", nodes, e);
            return null;
        }
    }

    @Bean
    public DistributedLockAspect mallDistributedLockAspect() {
        return new DistributedLockAspect();
    }

}
