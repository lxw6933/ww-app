# Kafka Starter 组件

封装Kafka操作的Spring Boot Starter组件，提供高性能、高扩展性的Kafka操作接口。

## 功能特性

- 简化Kafka配置，支持自动装配
- 提供高性能的生产者和消费者配置
- 支持String和JSON类型的消息发送和接收
- 支持同步和异步消息发送
- 支持批量消息处理
- 提供易用的监听器抽象类
- 提供主题管理工具类
- 支持事务消息
- 增强的错误处理和重试机制
- 支持自定义消息头
- 支持指定分区发送
- **【新增】** 消息追踪ID传递，支持分布式跟踪
- **【新增】** 死信队列自动处理
- **【新增】** 延迟消息发送功能
- **【新增】** 主题重平衡和压缩管理

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>com.ww.app</groupId>
    <artifactId>ww-spring-boot-starter-kafka</artifactId>
    <version>${revision}</version>
</dependency>
```

### 配置属性

在application.yml中添加配置：

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      # 生产者配置
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
      retries: 3
      batch-size: 16384
      buffer-memory: 33554432
      compression-type: snappy
      enable-idempotence: true
    consumer:
      # 消费者配置
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      group-id: default-group
      auto-offset-reset: earliest
      enable-auto-commit: false
      max-poll-records: 500
      # 错误处理配置
      error-handler-enabled: true
      safe-deserialization: true
      trusted-packages: com.ww.app
      retry:
        interval: 1000
        max-attempts: 3
      
# 自定义Kafka配置
ww:
  kafka:
    enabled: true
    retry-times: 3
    send-timeout: 10000
    compression-enabled: true
    compression-type: snappy
    transaction-enabled: false
    batch-size: 100
    consumer-concurrency: 3
    # 新增死信队列配置
    dead-letter:
      enabled: true
      suffix: ".dlq"
    # 新增追踪配置
    tracing:
      enabled: true
      propagate-trace-id: true
    producer:
      idempotence-enabled: true
      acks: all
      buffer-memory: 33554432
    consumer:
      group-id: default-group
      auto-commit: false
      batch-enabled: false
      concurrency: 3
```

### 发送消息示例

```java
@RestController
@RequestMapping("/kafka")
public class KafkaController {

    @Resource
    private KafkaService kafkaService;
    
    @PostMapping("/send")
    public String sendMessage(@RequestParam String topic, @RequestParam String message) {
        kafkaService.sendMessage(topic, message);
        return "发送成功";
    }
    
    @PostMapping("/send-json")
    public String sendJson(@RequestParam String topic, @RequestBody Object data) {
        kafkaService.sendObject(topic, data);
        return "发送成功";
    }
    
    @PostMapping("/send-with-headers")
    public String sendWithHeaders(@RequestParam String topic, 
                                 @RequestParam String message, 
                                 @RequestParam String key,
                                 @RequestBody Map<String, String> headers) {
        kafkaService.sendMessage(topic, key, message, headers);
        return "发送成功";
    }
    
    @PostMapping("/send-batch")
    public String sendBatch(@RequestParam String topic, @RequestBody List<String> messages) {
        List<SendResult<String, String>> results = kafkaService.sendBatchAndWait(topic, messages);
        return "成功发送 " + results.size() + " 条消息";
    }
    
    @PostMapping("/send-in-tx")
    public String sendInTransaction(@RequestParam String topic, 
                                  @RequestParam String message1,
                                  @RequestParam String message2) {
        kafkaService.executeInTransaction(operations -> {
            operations.send(topic, "tx-key-1", message1);
            operations.send(topic, "tx-key-2", message2);
            return true;
        });
        return "事务发送成功";
    }
    
    // 【新增】 延迟消息发送示例
    @PostMapping("/send-delayed")
    public String sendDelayed(@RequestParam String topic, 
                             @RequestParam String message,
                             @RequestParam long delayMillis) {
        kafkaService.sendDelayedMessage(topic, message, delayMillis);
        return "延迟消息调度成功";
    }
    
    // 【新增】 定时消息发送示例
    @PostMapping("/send-at-time")
    public String sendAtTime(@RequestParam String topic, 
                            @RequestParam String message,
                            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date scheduledTime) {
        kafkaService.sendAtTime(topic, message, scheduledTime);
        return "定时消息调度成功";
    }
}
```

### 消费消息示例

```java
@Component
public class OrderMessageListener extends AbstractKafkaListener {

    @KafkaListener(topics = "order-topic", containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        processRecord(record, ack);
    }
    
    @Override
    protected void beforeProcess(ConsumerRecord<String, String> record) throws Exception {
        // 前置处理，例如日志记录、消息验证等
        Map<String, String> headers = extractHeaders(record);
        if (headers.containsKey("X-Priority") && "HIGH".equals(headers.get("X-Priority"))) {
            log.info("接收到高优先级消息: {}", record.key());
        }
    }
    
    @Override
    protected void processMessage(String key, String value, ConsumerRecord<String, String> record) throws Exception {
        // 业务处理逻辑
        log.info("处理订单消息: {}", value);
    }
    
    @Override
    protected boolean shouldRetry(Exception e) {
        // 自定义重试逻辑
        return !(e instanceof IllegalArgumentException) && !(e instanceof KafkaException);
    }
    
    @Override
    protected boolean isDeadLetterQueueEnabled() {
        // 启用死信队列处理
        return true;
    }
    
    @Override
    protected int getMaxRetries() {
        // 设置最大重试次数
        return 3;
    }
    
    @Override
    protected void onError(ConsumerRecord<String, String> record, Exception e) {
        // 错误处理逻辑，例如发送告警、记录失败消息等
        log.error("消息处理失败，将记录到错误日志: {}", record.key());
    }
}
```

### 批量消费示例

```java
@Component
public class BatchOrderListener extends AbstractKafkaListener {

    @KafkaListener(topics = "batch-order-topic", containerFactory = "batchKafkaListenerContainerFactory")
    public void onMessage(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        processBatchRecords(records, ack);
    }
    
    @Override
    protected void beforeBatchProcess(List<ConsumerRecord<String, String>> records) throws Exception {
        // 批量前置处理
        log.info("开始处理批量消息, 批次大小: {}", records.size());
    }
    
    @Override
    protected void processMessages(List<ConsumerRecord<String, String>> records) throws Exception {
        // 提取所有消息值
        List<String> orderIds = extractValues(records);
        
        // 批量处理订单ID列表
        processBatchOrders(orderIds);
    }
    
    private void processBatchOrders(List<String> orderIds) {
        // 业务处理逻辑
        log.info("批量处理 {} 个订单", orderIds.size());
    }
}
```

### 管理Kafka主题

```java
@Service
public class KafkaAdminService {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    public boolean createTopic(String topicName, int partitions, short replicationFactor) {
        try (AdminClient adminClient = KafkaAdminUtils.createAdminClient(bootstrapServers)) {
            return KafkaTopicUtils.createTopic(adminClient, topicName, partitions, replicationFactor);
        }
    }
    
    public boolean createTopicWithRetention(String topicName, int partitions, 
                                          short replicationFactor, long retentionMs) {
        try (AdminClient adminClient = KafkaAdminUtils.createAdminClient(bootstrapServers)) {
            return KafkaTopicUtils.createTopicWithRetention(
                adminClient, topicName, partitions, replicationFactor, retentionMs);
        }
    }
    
    // 【新增】创建压缩主题
    public boolean createCompactTopic(String topicName, int partitions, 
                                     short replicationFactor, long compactMinBytes) {
        try (AdminClient adminClient = KafkaAdminUtils.createAdminClient(bootstrapServers)) {
            return KafkaTopicUtils.createCompactTopic(
                adminClient, topicName, partitions, replicationFactor, compactMinBytes);
        }
    }
    
    public boolean deleteTopic(String topicName) {
        try (AdminClient adminClient = KafkaAdminUtils.createAdminClient(bootstrapServers)) {
            return KafkaTopicUtils.deleteTopic(adminClient, topicName);
        }
    }
    
    public Set<String> listAllTopics() {
        try (AdminClient adminClient = KafkaAdminUtils.createAdminClient(bootstrapServers)) {
            return KafkaTopicUtils.getAllTopics(adminClient);
        }
    }
    
    public boolean increasePartitions(String topicName, int totalPartitions) {
        try (AdminClient adminClient = KafkaAdminUtils.createAdminClient(bootstrapServers)) {
            return KafkaTopicUtils.increasePartitions(adminClient, topicName, totalPartitions);
        }
    }
    
    // 【新增】重平衡主题分区
    public CompletableFuture<Void> rebalanceTopicPartitions(String topicName) {
        try (AdminClient adminClient = KafkaAdminUtils.createAdminClient(bootstrapServers)) {
            return KafkaTopicUtils.rebalanceTopicPartitions(adminClient, topicName);
        }
    }
    
    // 【新增】修改主题保留时间
    public boolean setRetentionTime(String topicName, long retentionMs) {
        try (AdminClient adminClient = KafkaAdminUtils.createAdminClient(bootstrapServers)) {
            return KafkaTopicUtils.setRetentionTime(adminClient, topicName, retentionMs);
        }
    }
}
```

## 高级特性

### 事务消息

启用事务消息支持：

```yaml
spring:
  kafka:
    producer:
      transaction-id-prefix: tx-
ww:
  kafka:
    transaction-enabled: true
```

使用事务消息：

```java
@Resource
private KafkaOperations kafkaOperations;

public void sendMessagesInTransaction() {
    kafkaOperations.executeInTransaction(operations -> {
        operations.send("topic1", "message1");
        operations.send("topic2", "message2");
        return true;
    });
}
```

### 错误处理与重试

配置错误处理和重试机制：

```yaml
spring:
  kafka:
    consumer:
      error-handler-enabled: true
      retry:
        interval: 1000    # 重试间隔(毫秒)
        max-attempts: 3   # 最大重试次数
```

自定义监听器中的错误处理：

```java
@Override
protected boolean shouldRetry(Exception e) {
    // 系统异常重试，业务异常不重试
    return !(e instanceof BusinessException);
}

@Override
protected void onError(ConsumerRecord<String, String> record, Exception e) {
    // 错误处理逻辑，可以发送到死信队列或记录到数据库
    deadLetterService.sendToDeadLetterQueue(record, e);
}
```

### 【新增】死信队列处理

启用和配置死信队列：

```yaml
ww:
  kafka:
    dead-letter:
      enabled: true
      suffix: ".dlq"
      auto-create-topics: true
```

消费死信队列消息：

```java
@Component
public class DeadLetterQueueListener extends AbstractKafkaListener {

    @KafkaListener(topics = "order-topic.dlq", containerFactory = "kafkaListenerContainerFactory")
    public void onDeadLetterMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            // 提取原始消息的元数据
            Map<String, String> headers = extractHeaders(record);
            String originalTopic = headers.get("X-Original-Topic");
            String errorMessage = headers.get("X-Error-Message");
            String retryCount = headers.get("X-Retry-Count");
            
            log.info("处理死信队列消息: 原始主题={}, 错误={}, 重试次数={}", 
                    originalTopic, errorMessage, retryCount);
            
            // 处理死信队列消息的业务逻辑
            processDeadLetterMessage(record.key(), record.value(), originalTopic, errorMessage);
            
            // 确认消息
            ack.acknowledge();
        } catch (Exception e) {
            log.error("处理死信队列消息失败: {}", e.getMessage(), e);
            ack.acknowledge(); // 即使处理失败也确认，避免无限循环
        }
    }
    
    private void processDeadLetterMessage(String key, String value, String originalTopic, String errorMessage) {
        // 实现死信处理逻辑，例如保存到数据库、发送告警等
    }

    @Override
    protected void processMessage(String key, String value, ConsumerRecord<String, String> record) throws Exception {
        // 不会被调用，因为我们直接实现了onDeadLetterMessage方法
    }
}
```

### 【新增】延迟消息

使用延迟消息功能：

```java
// 发送延迟5秒的消息
kafkaService.sendDelayedMessage("delayed-topic", "这是一条延迟消息", 5000);

// 在指定时间发送消息
Date scheduledTime = new Date(System.currentTimeMillis() + 3600000); // 1小时后
kafkaService.sendAtTime("scheduled-topic", "这是一条定时消息", scheduledTime);
```

### 【新增】消息跟踪

默认情况下，系统会自动为每条消息生成唯一的跟踪ID，并在日志中包含此ID以便追踪。
如果您的系统中已有分布式跟踪框架（如SkyWalking, Zipkin等），可以集成现有的TraceId：

```java
// 自定义消息头中包含跟踪ID
Map<String, String> headers = new HashMap<>();
headers.put("X-Trace-Id", currentTraceId);
kafkaService.sendMessage("topic", "key", "message", headers);
```

在消费者端，跟踪ID会被自动提取并设置到MDC中，便于日志关联：

```java
@Override
protected void processMessage(String key, String value, ConsumerRecord<String, String> record) throws Exception {
    // 不需要手动设置，AbstractKafkaListener已自动处理
    // 日志中会自动包含traceId
    log.info("处理消息: {}", value);  // 输出如: [traceId=abc123] 处理消息: xxxxx
}
```

### 安全反序列化

启用安全反序列化机制，防止恶意数据导致的序列化问题：

```yaml
spring:
  kafka:
    consumer:
      safe-deserialization: true
      trusted-packages: com.ww.app,org.springframework
```

## 注意事项

- 确保Kafka服务器已经正确配置和启动
- 生产环境中建议启用消息压缩和幂等性
- 根据业务需求调整消费者并发数
- 建议使用手动确认消息模式确保消息不丢失
- 配置适当的重试机制处理临时错误
- 对于重要业务消息，建议使用事务消息确保一致性
- 使用适当的分区策略提高并行处理能力
- 【新增】对于需要保留键值状态的消息，使用压缩主题
- 【新增】定期检查并重平衡分区，确保集群负载均衡
- 【新增】合理设置死信队列机制，不要让失败消息无限重试

## 性能优化

为获得最佳性能，推荐以下配置：

### 生产者优化

```yaml
spring:
  kafka:
    producer:
      # 增大批处理大小
      batch-size: 32768
      # 增加批处理停留时间以积累更多消息
      linger-ms: 5
      # 使用压缩减少网络带宽
      compression-type: snappy
      # 增加缓冲区大小
      buffer-memory: 67108864
      # 启用幂等性避免消息重复
      enable-idempotence: true
```

### 消费者优化

```yaml
spring:
  kafka:
    consumer:
      # 增大批次获取数量
      max-poll-records: 1000
      # 调整消费者缓冲区
      fetch-max-bytes: 52428800
      # 消费者数量与分区数匹配
```

## 后续开发计划

本模块的后续开发和优化方向：

1. **集群监控**：集成监控指标收集，支持将消息处理统计数据导出到监控系统（如Prometheus）

2. **Schema管理**：支持Avro、Protobuf等Schema注册中心集成，实现消息格式版本控制

3. **消息流处理**：集成轻量级流处理能力，支持简单的消息转换和过滤

4. **多集群管理**：支持多Kafka集群配置和切换，实现跨集群消息转发

5. **消息回溯**：增加消费者组重置消费位点功能，支持消息回溯处理

6. **消息存档**：支持重要消息自动归档到数据库或存储系统

7. **配置中心集成**：与配置中心（如Nacos、Apollo）集成，实现配置动态更新

8. **安全增强**：增加SSL和SASL安全连接支持

## 贡献指南

欢迎为ww-spring-boot-starter-kafka模块贡献代码或提出改进建议。贡献流程如下：

1. Fork项目到自己的仓库
2. 创建功能分支（git checkout -b feature/xxx）
3. 提交变更（git commit -m 'Add xxx feature'）
4. 推送到远程分支（git push origin feature/xxx）
5. 创建Pull Request

## 许可证

本模块遵循项目的许可协议分发和使用。 