package com.ww.app.redis.config;

import com.ww.app.redis.aspect.DistributedLockAspect;
import com.ww.app.redis.codec.RedissonCodec;
import com.ww.app.redis.component.RedissonComponent;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author ww
 * @create 2023-07-15 17:22
 * @description: Redisson 自动配置，支持多种 Redis 部署模式
 */
@Slf4j
@ConditionalOnClass({RedisTemplate.class})
@EnableConfigurationProperties(RedisProperties.class)
public class RedissonAutoConfiguration {

    /**
     * Redis 正常连接前缀
     */
    private static final String REDISSON_PREFIX = "redis://";

    /**
     * Redis SSL 连接前缀
     */
    private static final String REDISSON_SSL_PREFIX = "rediss://";

    @Resource
    private RedisProperties redisProperties;

    /**
     * 创建 RedissonClient Bean
     * 自动识别 Redis 部署模式并配置
     *
     * @return RedissonClient 实例
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        try {
            // 1. 优先判断集群模式
            if (redisProperties.getCluster() != null
                    && redisProperties.getCluster().getNodes() != null
                    && !redisProperties.getCluster().getNodes().isEmpty()) {

                log.info("检测到 Redis 集群模式配置");
                configureCluster(config);

                // 2. 判断哨兵模式
            } else if (redisProperties.getSentinel() != null
                    && redisProperties.getSentinel().getNodes() != null
                    && !redisProperties.getSentinel().getNodes().isEmpty()) {

                log.info("检测到 Redis 哨兵模式配置");
                configureSentinel(config);

                // 3. 默认单机模式（包括主从模式，由 Redis 自身处理）
            } else {
                log.info("使用 Redis 单机模式配置");
                configureSingle(config);
            }

            // 设置序列化编解码器
            config.setCodec(new RedissonCodec());

            log.info("Redisson 初始化成功");
            return Redisson.create(config);
        } catch (Exception e) {
            log.error("Redisson 初始化失败", e);
            throw new RuntimeException("Redisson 初始化失败", e);
        }
    }

    /**
     * 配置单机模式
     *
     * @param config Redisson 配置对象
     */
    private void configureSingle(Config config) {
        String host = redisProperties.getHost();
        int port = redisProperties.getPort();
        String password = redisProperties.getPassword();
        int database = redisProperties.getDatabase();

        String address = REDISSON_PREFIX + host + ":" + port;

        config.useSingleServer()
                .setAddress(address)
                .setDatabase(database)
                .setPassword(StringUtils.hasText(password) ? password : null)
                .setConnectionPoolSize(getConnectionPoolSize())
                .setConnectionMinimumIdleSize(getConnectionMinimumIdleSize())
                .setTimeout((int) redisProperties.getTimeout().toMillis())
                .setConnectTimeout((int) redisProperties.getConnectTimeout().toMillis());

        log.info("单机模式配置: address={}, database={}", address, database);
    }

    /**
     * 配置哨兵模式
     *
     * @param config Redisson 配置对象
     */
    private void configureSentinel(Config config) {
        RedisProperties.Sentinel sentinel = redisProperties.getSentinel();
        String masterName = sentinel.getMaster();
        List<String> nodes = sentinel.getNodes();
        String password = redisProperties.getPassword();
        int database = redisProperties.getDatabase();

        // 转换节点地址格式
        String[] sentinelAddresses = nodes.stream()
                .map(node -> REDISSON_PREFIX + node)
                .toArray(String[]::new);

        config.useSentinelServers()
                .setMasterName(masterName)
                .addSentinelAddress(sentinelAddresses)
                .setDatabase(database)
                .setPassword(StringUtils.hasText(password) ? password : null)
                .setMasterConnectionPoolSize(getConnectionPoolSize())
                .setSlaveConnectionPoolSize(getConnectionPoolSize())
                .setMasterConnectionMinimumIdleSize(getConnectionMinimumIdleSize())
                .setSlaveConnectionMinimumIdleSize(getConnectionMinimumIdleSize())
                .setTimeout((int) redisProperties.getTimeout().toMillis())
                .setConnectTimeout((int) redisProperties.getConnectTimeout().toMillis())
                // 读写分离模式：读操作在从节点执行，写操作在主节点执行
                .setReadMode(org.redisson.config.ReadMode.SLAVE)
                .setSubscriptionMode(org.redisson.config.SubscriptionMode.MASTER);

        log.info("哨兵模式配置: masterName={}, sentinels={}, database={}",
                masterName, nodes, database);
    }

    /**
     * 配置集群模式
     *
     * @param config Redisson 配置对象
     */
    private void configureCluster(Config config) {
        RedisProperties.Cluster cluster = redisProperties.getCluster();
        List<String> nodes = cluster.getNodes();
        String password = redisProperties.getPassword();

        // 转换节点地址格式
        String[] clusterAddresses = nodes.stream()
                .map(node -> {
                    // 如果节点地址不包含 redis:// 前缀，则添加
                    if (!node.startsWith(REDISSON_PREFIX) && !node.startsWith(REDISSON_SSL_PREFIX)) {
                        return REDISSON_PREFIX + node;
                    }
                    return node;
                })
                .toArray(String[]::new);

        config.useClusterServers()
                .addNodeAddress(clusterAddresses)
                .setPassword(StringUtils.hasText(password) ? password : null)
                .setMasterConnectionPoolSize(getConnectionPoolSize())
                .setSlaveConnectionPoolSize(getConnectionPoolSize())
                .setMasterConnectionMinimumIdleSize(getConnectionMinimumIdleSize())
                .setSlaveConnectionMinimumIdleSize(getConnectionMinimumIdleSize())
                .setTimeout((int) redisProperties.getTimeout().toMillis())
                .setConnectTimeout((int) redisProperties.getConnectTimeout().toMillis())
                // 读写分离模式
                .setReadMode(org.redisson.config.ReadMode.SLAVE)
                .setSubscriptionMode(org.redisson.config.SubscriptionMode.MASTER)
                // 集群扫描间隔时间
                .setScanInterval(2000);

        log.info("集群模式配置: nodes={}", nodes);
    }

    /**
     * 获取连接池大小
     * 从 lettuce 或 jedis 配置中获取，如果没有配置则使用默认值
     *
     * @return 连接池大小
     */
    private int getConnectionPoolSize() {
        // 尝试从 lettuce 配置获取
        if (redisProperties.getLettuce() != null
                && redisProperties.getLettuce().getPool() != null) {
            int maxActive = redisProperties.getLettuce().getPool().getMaxActive();
            if (maxActive > 0) {
                return maxActive;
            }
        }

        // 尝试从 jedis 配置获取
        if (redisProperties.getJedis() != null
                && redisProperties.getJedis().getPool() != null) {
            int maxActive = redisProperties.getJedis().getPool().getMaxActive();
            if (maxActive > 0) {
                return maxActive;
            }
        }

        // 默认值：64
        return 64;
    }

    /**
     * 获取最小空闲连接数
     *
     * @return 最小空闲连接数
     */
    private int getConnectionMinimumIdleSize() {
        // 尝试从 lettuce 配置获取
        if (redisProperties.getLettuce() != null
                && redisProperties.getLettuce().getPool() != null) {
            int minIdle = redisProperties.getLettuce().getPool().getMinIdle();
            if (minIdle > 0) {
                return minIdle;
            }
        }

        // 尝试从 jedis 配置获取
        if (redisProperties.getJedis() != null
                && redisProperties.getJedis().getPool() != null) {
            int minIdle = redisProperties.getJedis().getPool().getMinIdle();
            if (minIdle > 0) {
                return minIdle;
            }
        }

        // 默认值：24
        return 24;
    }

    /**
     * 创建 RedissonComponent Bean
     *
     * @param redissonClient RedissonClient 实例
     * @return RedissonComponent 实例
     */
    @Bean
    public RedissonComponent redissonComponent(RedissonClient redissonClient) {
        log.info("创建 RedissonComponent Bean");
        return new RedissonComponent(redissonClient);
    }

    /**
     * 创建分布式锁切面 Bean
     *
     * @return DistributedLockAspect 实例
     */
    @Bean
    public DistributedLockAspect distributedLockAspect() {
        log.info("创建 DistributedLockAspect Bean");
        return new DistributedLockAspect();
    }
}
