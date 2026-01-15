package com.ww.app.redis.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;

import java.time.Duration;

/**
 * @author ww
 * @create 2025-04-02- 11:14
 * @description: Lettuce连接池配置
 */
@Configuration
@EnableConfigurationProperties(RedisProperties.class)
public class RedisPoolAutoConfiguration {

    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory(RedisProperties redisProperties) {
        LettuceClientConfiguration clientConfig = createLettuceClientConfiguration(redisProperties);
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(
                redisProperties.getHost(), redisProperties.getPort());
        if (StringUtils.isNotEmpty(redisProperties.getPassword())) {
            serverConfig.setPassword(redisProperties.getPassword());
        }
        if (redisProperties.getDatabase() != 0) {
            serverConfig.setDatabase(redisProperties.getDatabase());
        }
        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }

    private LettuceClientConfiguration createLettuceClientConfiguration(RedisProperties redisProperties) {
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder =
                LettucePoolingClientConfiguration.builder();
        // 锁只读主库【解决主从情况下，锁失效问题】
        builder.readFrom(io.lettuce.core.ReadFrom.MASTER_PREFERRED);
        // 配置连接池
        GenericObjectPoolConfig<?> poolConfig = getGenericObjectPoolConfig(redisProperties);
        builder.poolConfig(poolConfig);
        // 配置命令超时
        builder.commandTimeout(Duration.ofMillis(5000));
        // 在关闭客户端连接之前确保所有未完成的命令执行完成
        builder.shutdownTimeout(Duration.ofSeconds(2));
        // 开启TCP保活机制，防止连接因长时间不活动而被中间设备断开
        builder.clientOptions(
                io.lettuce.core.ClientOptions.builder()
                        .disconnectedBehavior(io.lettuce.core.ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                        .socketOptions(
                                io.lettuce.core.SocketOptions.builder()
                                        .keepAlive(true)
                                        .tcpNoDelay(true)
                                        .build()
                        )
                        .build()
        );
        return builder.build();
    }

    private static GenericObjectPoolConfig<?> getGenericObjectPoolConfig(RedisProperties redisProperties) {
        RedisProperties.Pool pool = redisProperties.getLettuce().getPool();
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(pool.getMaxActive());
        poolConfig.setMaxIdle(pool.getMaxIdle());
        poolConfig.setMinIdle(pool.getMinIdle());
        poolConfig.setMaxWait(pool.getMaxWait());

        // 空闲连接检测
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTime(Duration.ofMillis(60000));
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofMillis(30000));
        poolConfig.setNumTestsPerEvictionRun(3);
        return poolConfig;
    }

}
