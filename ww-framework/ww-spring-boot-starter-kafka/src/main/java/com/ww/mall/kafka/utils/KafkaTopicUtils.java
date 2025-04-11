package com.ww.mall.kafka.utils;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Kafka主题工具类
 */
public class KafkaTopicUtils {
    private static final Logger log = LoggerFactory.getLogger(KafkaTopicUtils.class);
    
    // 默认超时时间(秒)
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    
    // 清理策略常量
    public static final String CLEANUP_POLICY_DELETE = TopicConfig.CLEANUP_POLICY_DELETE;  // 删除策略
    public static final String CLEANUP_POLICY_COMPACT = TopicConfig.CLEANUP_POLICY_COMPACT;  // 压缩策略
    public static final String CLEANUP_POLICY_COMPACT_DELETE = TopicConfig.CLEANUP_POLICY_COMPACT + "," + TopicConfig.CLEANUP_POLICY_DELETE;  // 混合策略
    
    /**
     * 创建主题
     *
     * @param adminClient Kafka管理客户端
     * @param topicName 主题名称
     * @param partitions 分区数
     * @param replicationFactor 副本因子
     * @param configs 主题配置
     * @return 是否创建成功
     */
    public static boolean createTopic(AdminClient adminClient, String topicName, int partitions, 
                                      short replicationFactor, Map<String, String> configs) {
        try {
            NewTopic newTopic = new NewTopic(topicName, partitions, replicationFactor);
            
            if (configs != null && !configs.isEmpty()) {
                newTopic.configs(configs);
            }
            
            CreateTopicsResult result = adminClient.createTopics(Collections.singleton(newTopic));
            result.all().get(30, TimeUnit.SECONDS);
            log.info("成功创建主题: {}", topicName);
            return true;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("创建主题失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 创建主题（使用默认配置）
     *
     * @param adminClient Kafka管理客户端
     * @param topicName 主题名称
     * @param partitions 分区数
     * @param replicationFactor 副本因子
     * @return 是否创建成功
     */
    public static boolean createTopic(AdminClient adminClient, String topicName, int partitions, 
                                      short replicationFactor) {
        return createTopic(adminClient, topicName, partitions, replicationFactor, null);
    }
    
    /**
     * 删除主题
     *
     * @param adminClient Kafka管理客户端
     * @param topicName 主题名称
     * @return 是否删除成功
     */
    public static boolean deleteTopic(AdminClient adminClient, String topicName) {
        try {
            DeleteTopicsResult result = adminClient.deleteTopics(Collections.singleton(topicName));
            result.all().get(30, TimeUnit.SECONDS);
            log.info("成功删除主题: {}", topicName);
            return true;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("删除主题失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 判断主题是否存在
     *
     * @param adminClient Kafka管理客户端
     * @param topicName 主题名称
     * @return 主题是否存在
     */
    public static boolean topicExists(AdminClient adminClient, String topicName) {
        try {
            ListTopicsResult listTopicsResult = adminClient.listTopics();
            Set<String> topicNames = listTopicsResult.names().get(30, TimeUnit.SECONDS);
            return topicNames.contains(topicName);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("检查主题是否存在失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取所有主题
     *
     * @param adminClient Kafka管理客户端
     * @return 主题集合
     */
    public static Set<String> getAllTopics(AdminClient adminClient) {
        try {
            ListTopicsResult listTopicsResult = adminClient.listTopics();
            return listTopicsResult.names().get(30, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("获取所有主题失败: {}", e.getMessage(), e);
            return Collections.emptySet();
        }
    }
    
    /**
     * 更新主题配置
     *
     * @param adminClient Kafka管理客户端
     * @param topicName 主题名称
     * @param configs 主题配置
     * @return 是否更新成功
     */
    public static boolean updateTopicConfig(AdminClient adminClient, String topicName, 
                                            Map<String, String> configs) {
        try {
            ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
            List<AlterConfigOp> configOps = new ArrayList<>();
            
            for (Map.Entry<String, String> entry : configs.entrySet()) {
                configOps.add(new AlterConfigOp(
                        new ConfigEntry(entry.getKey(), entry.getValue()),
                        AlterConfigOp.OpType.SET
                ));
            }
            
            Map<ConfigResource, Collection<AlterConfigOp>> configMap = new HashMap<>();
            configMap.put(resource, configOps);
            
            AlterConfigsResult result = adminClient.incrementalAlterConfigs(configMap);
            result.all().get(30, TimeUnit.SECONDS);
            log.info("成功更新主题配置: {}", topicName);
            return true;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("更新主题配置失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取主题配置
     *
     * @param adminClient Kafka管理客户端
     * @param topicName 主题名称
     * @return 主题配置
     */
    public static Map<String, String> getTopicConfig(AdminClient adminClient, String topicName) {
        try {
            ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
            DescribeConfigsResult result = adminClient.describeConfigs(Collections.singleton(resource));
            Config config = result.all().get(30, TimeUnit.SECONDS).get(resource);
            
            Map<String, String> configMap = new HashMap<>();
            for (ConfigEntry entry : config.entries()) {
                configMap.put(entry.name(), entry.value());
            }
            
            return configMap;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("获取主题配置失败: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * 增加主题分区
     *
     * @param adminClient Kafka管理客户端
     * @param topicName 主题名称
     * @param totalPartitions 总分区数
     * @return 是否增加成功
     */
    public static boolean increasePartitions(AdminClient adminClient, String topicName, int totalPartitions) {
        try {
            Map<String, NewPartitions> newPartitionsMap = new HashMap<>();
            newPartitionsMap.put(topicName, NewPartitions.increaseTo(totalPartitions));
            
            CreatePartitionsResult result = adminClient.createPartitions(newPartitionsMap);
            result.all().get(30, TimeUnit.SECONDS);
            log.info("成功增加主题分区: {} -> {}", topicName, totalPartitions);
            return true;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("增加主题分区失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 创建带有常用配置的主题
     *
     * @param adminClient Kafka管理客户端
     * @param topicName 主题名称
     * @param partitions 分区数
     * @param replicationFactor 副本因子
     * @param retentionMs 消息保留时间（毫秒）
     * @return 是否创建成功
     */
    public static boolean createTopicWithRetention(AdminClient adminClient, String topicName, 
                                                  int partitions, short replicationFactor, 
                                                  long retentionMs) {
        Map<String, String> configs = new HashMap<>();
        configs.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retentionMs));
        return createTopic(adminClient, topicName, partitions, replicationFactor, configs);
    }
    
    /**
     * 创建带有压缩策略的主题
     *
     * @param adminClient Kafka管理客户端
     * @param topicName 主题名称
     * @param partitions 分区数
     * @param replicationFactor 副本因子
     * @param compactMinBytes 最小压缩大小
     * @return 是否创建成功
     */
    public static boolean createCompactTopic(AdminClient adminClient, String topicName, int partitions,
                                           short replicationFactor, long compactMinBytes) {
        Map<String, String> configs = new HashMap<>();
        configs.put(TopicConfig.CLEANUP_POLICY_CONFIG, CLEANUP_POLICY_COMPACT);
        configs.put(TopicConfig.MIN_COMPACTION_LAG_MS_CONFIG, "0");
        configs.put(TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_CONFIG, "0.01");
        
        // 设置压缩大小
        if (compactMinBytes > 0) {
            configs.put(TopicConfig.MIN_COMPACTION_LAG_MS_CONFIG, String.valueOf(compactMinBytes));
        }
        
        return createTopic(adminClient, topicName, partitions, replicationFactor, configs);
    }
    
    /**
     * 创建带有混合清理策略的主题
     *
     * @param adminClient Kafka管理客户端
     * @param topicName 主题名称
     * @param partitions 分区数
     * @param replicationFactor 副本因子
     * @param retentionMs 消息保留时间(毫秒)
     * @return 是否创建成功
     */
    public static boolean createCompactAndDeleteTopic(AdminClient adminClient, String topicName, int partitions,
                                                    short replicationFactor, long retentionMs) {
        Map<String, String> configs = new HashMap<>();
        configs.put(TopicConfig.CLEANUP_POLICY_CONFIG, CLEANUP_POLICY_COMPACT_DELETE);
        configs.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retentionMs));
        
        return createTopic(adminClient, topicName, partitions, replicationFactor, configs);
    }
    
    /**
     * 获取主题详情信息
     *
     * @param adminClient Kafka管理客户端
     * @param topicName 主题名称
     * @return 主题详情
     */
    public static TopicDescription getTopicDescription(AdminClient adminClient, String topicName) {
        try {
            DescribeTopicsResult result = adminClient.describeTopics(Collections.singleton(topicName));
            Map<String, TopicDescription> topicDescriptions = result.all().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return topicDescriptions.get(topicName);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("获取主题详情失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 获取主题分区信息
     *
     * @param adminClient Kafka管理客户端
     * @param topicName 主题名称
     * @return 分区信息列表
     */
    public static List<TopicPartitionInfo> getTopicPartitions(AdminClient adminClient, String topicName) {
        TopicDescription description = getTopicDescription(adminClient, topicName);
        return description != null ? description.partitions() : Collections.emptyList();
    }
    
    /**
     * 变更主题清理策略
     *
     * @param adminClient Kafka管理客户端
     * @param topicName 主题名称
     * @param cleanupPolicy 清理策略
     * @return 是否成功
     */
    public static boolean changeCleanupPolicy(AdminClient adminClient, String topicName, String cleanupPolicy) {
        Map<String, String> configs = new HashMap<>();
        configs.put(TopicConfig.CLEANUP_POLICY_CONFIG, cleanupPolicy);
        return updateTopicConfig(adminClient, topicName, configs);
    }
    
    /**
     * 设置主题消息过期时间
     *
     * @param adminClient Kafka管理客户端
     * @param topicName 主题名称
     * @param retentionMs 保留时间(毫秒)
     * @return 是否成功
     */
    public static boolean setRetentionTime(AdminClient adminClient, String topicName, long retentionMs) {
        Map<String, String> configs = new HashMap<>();
        configs.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retentionMs));
        return updateTopicConfig(adminClient, topicName, configs);
    }
    
    /**
     * 设置主题消息大小
     *
     * @param adminClient Kafka管理客户端
     * @param topicName 主题名称
     * @param maxMessageBytes 最大消息大小(字节)
     * @return 是否成功
     */
    public static boolean setMaxMessageSize(AdminClient adminClient, String topicName, int maxMessageBytes) {
        Map<String, String> configs = new HashMap<>();
        configs.put(TopicConfig.MAX_MESSAGE_BYTES_CONFIG, String.valueOf(maxMessageBytes));
        return updateTopicConfig(adminClient, topicName, configs);
    }
    
    /**
     * 重新平衡主题分区
     * 注意：此操作会重新分配分区并可能导致数据迁移，可能影响性能
     *
     * @param adminClient Kafka管理客户端
     * @param topicName 主题名称
     * @return 操作结果的Future
     */
    public static CompletableFuture<Void> rebalanceTopicPartitions(AdminClient adminClient, String topicName) {
        CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        
        try {
            // 1. 获取主题分区信息
            TopicDescription topicDescription = getTopicDescription(adminClient, topicName);
            if (topicDescription == null) {
                resultFuture.completeExceptionally(new RuntimeException("主题不存在: " + topicName));
                return resultFuture;
            }
            
            int numPartitions = topicDescription.partitions().size();
            
            // 2. 获取所有可用的broker
            DescribeClusterResult clusterResult = adminClient.describeCluster();
            Collection<Node> nodes = clusterResult.nodes().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            List<Integer> allBrokerIds = nodes.stream()
                    .map(Node::id)
                    .collect(Collectors.toList());
            
            if (allBrokerIds.isEmpty()) {
                resultFuture.completeExceptionally(new RuntimeException("没有可用的Broker节点"));
                return resultFuture;
            }
            
            // 3. 计算最优分配方案
            Map<org.apache.kafka.common.TopicPartition, Optional<NewPartitionReassignment>> newAssignments = new HashMap<>();
            
            for (int partitionId = 0; partitionId < numPartitions; partitionId++) {
                TopicPartitionInfo partitionInfo = topicDescription.partitions().get(partitionId);
                int replicationFactor = partitionInfo.replicas().size();
                
                // 为每个分区创建新的replica分配
                List<Integer> newReplicas = new ArrayList<>();
                for (int i = 0; i < replicationFactor; i++) {
                    // 选择相对均匀的broker
                    int brokerIndex = (partitionId + i) % allBrokerIds.size();
                    newReplicas.add(allBrokerIds.get(brokerIndex));
                }
                
                org.apache.kafka.common.TopicPartition topicPartition = 
                        new org.apache.kafka.common.TopicPartition(topicName, partitionId);
                
                NewPartitionReassignment reassignment = new NewPartitionReassignment(newReplicas);
                newAssignments.put(topicPartition, Optional.of(reassignment));
            }
            
            // 4. 执行重新分配
            AlterPartitionReassignmentsResult reassignResult = 
                    adminClient.alterPartitionReassignments(newAssignments);
            
            // 5. 转换为CompletableFuture
            KafkaFuture<Void> kafkaFuture = reassignResult.all();
            kafkaFuture.whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("重新平衡主题分区失败: {}", exception.getMessage(), exception);
                    resultFuture.completeExceptionally(exception);
                } else {
                    log.info("成功重新平衡主题分区: {}", topicName);
                    resultFuture.complete(null);
                }
            });
            
            return resultFuture;
        } catch (Exception e) {
            log.error("启动主题重新平衡失败: {}", e.getMessage(), e);
            resultFuture.completeExceptionally(e);
            return resultFuture;
        }
    }
    
    /**
     * 监控分区重新平衡的进度
     *
     * @param adminClient Kafka管理客户端
     * @param topicName 主题名称
     * @return 分区重新分配的状态
     */
    public static Map<org.apache.kafka.common.TopicPartition, PartitionReassignment> checkReassignmentStatus(
            AdminClient adminClient, String topicName) {
        try {
            TopicDescription desc = getTopicDescription(adminClient, topicName);
            if (desc == null) {
                return Collections.emptyMap();
            }
            
            Set<org.apache.kafka.common.TopicPartition> partitions = desc.partitions().stream()
                    .map(p -> new org.apache.kafka.common.TopicPartition(topicName, p.partition()))
                    .collect(Collectors.toSet());
            
            ListPartitionReassignmentsResult result = adminClient.listPartitionReassignments(partitions);
            return result.reassignments().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("获取分区重新分配状态失败: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
} 