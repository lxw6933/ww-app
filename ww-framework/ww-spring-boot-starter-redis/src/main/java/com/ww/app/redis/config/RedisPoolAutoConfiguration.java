package com.ww.app.redis.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
        // 根据配置类型自动选择：集群 > 哨兵 > 单机
        LettuceClientConfiguration clientConfig = createLettuceClientConfiguration(redisProperties);
        if (isCluster(redisProperties)) {
            RedisClusterConfiguration clusterConfig = createClusterConfiguration(redisProperties);
            return new LettuceConnectionFactory(clusterConfig, clientConfig);
        }
        if (isSentinel(redisProperties)) {
            RedisSentinelConfiguration sentinelConfig = createSentinelConfiguration(redisProperties);
            return new LettuceConnectionFactory(sentinelConfig, clientConfig);
        }
        RedisStandaloneConfiguration serverConfig = createStandaloneConfiguration(redisProperties);
        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }

    private LettuceClientConfiguration createLettuceClientConfiguration(RedisProperties redisProperties) {
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder =
                LettucePoolingClientConfiguration.builder();
        // 锁只读主库【解决主从情况下，锁失效问题】
        builder.readFrom(ReadFrom.MASTER_PREFERRED);
        // 配置连接池
        GenericObjectPoolConfig<?> poolConfig = getGenericObjectPoolConfig(redisProperties);
        builder.poolConfig(poolConfig);
        // 配置命令超时
        Duration timeout = redisProperties.getTimeout();
        builder.commandTimeout(timeout != null ? timeout : Duration.ofMillis(5000));
        // 在关闭客户端连接之前确保所有未完成的命令执行完成
        builder.shutdownTimeout(Duration.ofSeconds(2));
        // TLS
        if (redisProperties.isSsl()) {
            builder.useSsl();
        }
        // 客户端名称
        if (StringUtils.isNotBlank(redisProperties.getClientName())) {
            builder.clientName(redisProperties.getClientName());
        }
        // 开启TCP保活机制，防止连接因长时间不活动而被中间设备断开
        builder.clientOptions(buildClientOptions(redisProperties));
        return builder.build();
    }

    private ClientOptions buildClientOptions(RedisProperties redisProperties) {
        ClientOptions.Builder base = ClientOptions.builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .socketOptions(
                        SocketOptions.builder()
                                .keepAlive(true)
                                .tcpNoDelay(true)
                                .build()
                );
        if (isCluster(redisProperties)) {
            // 集群开启拓扑刷新，避免节点变更导致路由失效
            ClusterTopologyRefreshOptions refreshOptions = ClusterTopologyRefreshOptions.builder()
                    .enablePeriodicRefresh(Duration.ofSeconds(30))
                    .enableAllAdaptiveRefreshTriggers()
                    .build();
            return ClusterClientOptions.builder()
                    .topologyRefreshOptions(refreshOptions)
                    .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                    .socketOptions(
                            SocketOptions.builder()
                                    .keepAlive(true)
                                    .tcpNoDelay(true)
                                    .build()
                    )
                    .build();
        }
        return base.build();
    }

    private boolean isCluster(RedisProperties redisProperties) {
        RedisProperties.Cluster cluster = redisProperties.getCluster();
        return cluster != null && cluster.getNodes() != null && !cluster.getNodes().isEmpty();
    }

    private boolean isSentinel(RedisProperties redisProperties) {
        RedisProperties.Sentinel sentinel = redisProperties.getSentinel();
        return sentinel != null && StringUtils.isNotBlank(sentinel.getMaster())
                && sentinel.getNodes() != null && !sentinel.getNodes().isEmpty();
    }

    private RedisStandaloneConfiguration createStandaloneConfiguration(RedisProperties redisProperties) {
        // 单机配置
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(
                redisProperties.getHost(), redisProperties.getPort());
        applyAuthentication(serverConfig, redisProperties);
        if (redisProperties.getDatabase() != 0) {
            serverConfig.setDatabase(redisProperties.getDatabase());
        }
        return serverConfig;
    }

    private RedisSentinelConfiguration createSentinelConfiguration(RedisProperties redisProperties) {
        // 哨兵配置
        RedisProperties.Sentinel sentinel = redisProperties.getSentinel();
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();
        sentinelConfig.master(Objects.requireNonNull(sentinel).getMaster());
        sentinelConfig.setSentinels(parseSentinelNodes(sentinel.getNodes()));
        if (redisProperties.getDatabase() != 0) {
            sentinelConfig.setDatabase(redisProperties.getDatabase());
        }
        applyAuthentication(sentinelConfig, redisProperties);
        return sentinelConfig;
    }

    private RedisClusterConfiguration createClusterConfiguration(RedisProperties redisProperties) {
        // 集群配置
        RedisProperties.Cluster cluster = redisProperties.getCluster();
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(Objects.requireNonNull(cluster).getNodes());
        if (cluster.getMaxRedirects() != null) {
            clusterConfig.setMaxRedirects(cluster.getMaxRedirects());
        }
        applyAuthentication(clusterConfig, redisProperties);
        return clusterConfig;
    }

    private void applyAuthentication(RedisStandaloneConfiguration config, RedisProperties redisProperties) {
        // ACL 用户名与密码
        if (StringUtils.isNotBlank(redisProperties.getUsername())) {
            config.setUsername(redisProperties.getUsername());
        }
        if (StringUtils.isNotBlank(redisProperties.getPassword())) {
            config.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }
    }

    private void applyAuthentication(RedisSentinelConfiguration config, RedisProperties redisProperties) {
        // ACL 用户名与密码
        if (StringUtils.isNotBlank(redisProperties.getUsername())) {
            config.setUsername(redisProperties.getUsername());
        }
        if (StringUtils.isNotBlank(redisProperties.getPassword())) {
            config.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }
    }

    private void applyAuthentication(RedisClusterConfiguration config, RedisProperties redisProperties) {
        // ACL 用户名与密码
        if (StringUtils.isNotBlank(redisProperties.getUsername())) {
            config.setUsername(redisProperties.getUsername());
        }
        if (StringUtils.isNotBlank(redisProperties.getPassword())) {
            config.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }
    }

    private static GenericObjectPoolConfig<?> getGenericObjectPoolConfig(RedisProperties redisProperties) {
        RedisProperties.Lettuce lettuce = redisProperties.getLettuce();
        RedisProperties.Pool pool = lettuce == null ? null : lettuce.getPool();
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        if (pool != null) {
            poolConfig.setMaxTotal(pool.getMaxActive());
            poolConfig.setMaxIdle(pool.getMaxIdle());
            poolConfig.setMinIdle(pool.getMinIdle());
            poolConfig.setMaxWait(pool.getMaxWait());
        }

        // 空闲连接检测
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTime(Duration.ofMillis(60000));
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofMillis(30000));
        poolConfig.setNumTestsPerEvictionRun(3);
        return poolConfig;
    }

    private Set<RedisNode> parseSentinelNodes(List<String> nodes) {
        // 解析哨兵节点列表（host:port）
        Set<RedisNode> result = new HashSet<>();
        if (nodes == null || nodes.isEmpty()) {
            return result;
        }
        for (String node : nodes) {
            if (StringUtils.isBlank(node)) {
                continue;
            }
            String[] parts = StringUtils.split(node, ':');
            if (parts == null || parts.length != 2) {
                continue;
            }
            String host = parts[0].trim();
            int port = Integer.parseInt(parts[1].trim());
            result.add(new RedisNode(host, port));
        }
        return result;
    }

}
