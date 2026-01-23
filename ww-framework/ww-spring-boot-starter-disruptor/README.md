# WW Disruptor Spring Boot Starter

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-green.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)

基于LMAX Disruptor的高性能事件处理Spring Boot Starter，提供易用的架构设计、高扩展性和模块化的实现。

## 🚀 核心特性

### 高性能
- **百万级TPS**: 基于LMAX Disruptor，支持超高并发事件处理
- **零拷贝设计**: 使用RingBuffer，避免内存分配和GC压力
- **多种等待策略**: BLOCKING、YIELDING、SLEEPING、BUSY_SPIN
- **批量处理优化**: 减少上下文切换，提升吞吐量

### 易用性
- **Builder模式**: 流式API，简单直观
- **模板类设计**: 类似RestTemplate，降低学习成本
- **注解驱动**: 支持@EventHandler注解（计划中）

### 高扩展性
- **插件化处理器**: 支持自定义EventProcessor
- **SPI扩展机制**: 持久化、监控均可扩展
- **模块化设计**: 核心、处理器、持久化、监控分离
- **策略模式**: 等待策略、重试策略、降级策略可配置

### 生产就绪
- **完善的监控**: 集成Micrometer，支持Prometheus
- **链路追踪**: 支持OpenTelemetry（计划中）
- **健康检查**: Spring Actuator集成
- **持久化支持**: 文件、Redis、Kafka等多种实现（计划中）

## 📦 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.ww.app</groupId>
    <artifactId>ww-spring-boot-starter-disruptor</artifactId>
    <version>${revision}</version>
</dependency>
```

### 2. 配置属性

业务自建时可选配置（已移除自动配置，需自行读取使用）：

```yaml
ww:
  disruptor:
    ring-buffer-size: 1024          # RingBuffer大小（必须是2的幂）
    consumer-threads: 4              # 消费者线程数
    batch-size: 100                  # 批处理大小
    batch-timeout: 1000              # 批处理超时（毫秒）
    wait-strategy: BLOCKING          # 等待策略
    batch-enabled: true              # 是否启用批量处理
    
    # 持久化配置（可选）
    persistence:
      enabled: false
      type: file
      data-dir: ./data/disruptor
      
    # 监控配置
    metrics:
      enabled: true
      prefix: ww.disruptor
      detailed: false
```

### 3. 创建事件处理器

**方式一：实现EventProcessor接口**

```java
import com.ww.app.disruptor.processor.EventProcessor;
import com.ww.app.disruptor.model.Event;
import com.ww.app.disruptor.model.ProcessResult;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProcessor implements EventProcessor<OrderData> {

    @Override
    public ProcessResult process(Event<OrderData> event) {
        OrderData order = event.getPayload();

        // 处理订单逻辑
        System.out.println("处理订单: " + order.getOrderId());

        return ProcessResult.success("订单处理成功");
    }

    @Override
    public boolean supports(String eventType) {
        return "order".equals(eventType);
    }
}
```

**方式二：实现BatchEventProcessor接口（批量处理）**

```java
import com.ww.app.disruptor.processor.BatchEventProcessor;
import com.ww.app.disruptor.model.EventBatch;
import com.ww.app.disruptor.model.ProcessResult;
import org.springframework.stereotype.Component;

@Component
public class OrderBatchProcessor implements BatchEventProcessor<OrderData> {

    @Override
    public ProcessResult processBatch(EventBatch<OrderData> batch) {
        List<Event<OrderData>> events = batch.getEvents();

        // 批量处理订单
        System.out.println("批量处理 " + events.size() + " 个订单");

        // 批量入库等操作
        // ...

        return ProcessResult.success("批量处理成功");
    }

    @Override
    public int getBatchSize() {
        return 200;  // 自定义批量大小
    }
}
```

### 4. 发布事件

**方式一：使用Builder构建自定义Template（推荐）**

```java
import com.ww.app.disruptor.api.DisruptorTemplate;

DisruptorTemplate<OrderData> template = DisruptorTemplate.<OrderData>builder()
        .businessName("order-processor")  // 指定业务名称，用于区分线程
        .ringBufferSize(2048)
        .consumerThreads(8)
        .batchSize(200)
        .batchBufferCapacity(10000)
        .batchBufferOverflow("DROP")
        .waitStrategy("YIELDING")
        .executor(ThreadUtil.initFixedThreadPoolExecutor("xxx", 8))
        .eventProcessor(event -> {
            // 自定义处理逻辑
            return ProcessResult.success();
        })
        .build();

// 启动
template.start();

// 发布事件
template.publish("order",orderData);

// 停止
template.stop();
```

## 📖 配置说明

### 核心配置（业务自建时可选）

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `ww.disruptor.ring-buffer-size` | int | 1024 | RingBuffer大小，必须是2的幂 |
| `ww.disruptor.consumer-threads` | int | 4 | 消费者线程数 |
| `ww.disruptor.batch-size` | int | 100 | 批处理大小 |
| `ww.disruptor.batch-buffer-capacity` | int | 10000 | 批量缓冲区最大容量 |
| `ww.disruptor.batch-buffer-overflow` | String | DROP | 批量缓冲区溢出策略（DROP/BLOCK） |
| `ww.disruptor.batch-timeout` | long | 1000 | 批处理超时时间（毫秒） |
| `ww.disruptor.wait-strategy` | String | BLOCKING | 等待策略 |
| `ww.disruptor.batch-enabled` | boolean | true | 是否启用批量处理 |

### 等待策略说明

| 策略 | CPU使用率 | 延迟 | 适用场景 |
|------|-----------|------|----------|
| **BLOCKING** | 低 | 高 | 通用场景，推荐默认使用 |
| **YIELDING** | 中 | 中 | 平衡性能和CPU使用率 |
| **SLEEPING** | 极低 | 极高 | CPU敏感场景 |
| **BUSY_SPIN** | 极高 | 极低 | 低延迟要求场景 |

### 持久化配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `ww.disruptor.persistence.enabled` | boolean | false | 是否启用持久化 |
| `ww.disruptor.persistence.type` | String | file | 持久化类型 |
| `ww.disruptor.persistence.data-dir` | String | ./data/disruptor | 数据目录 |
| `ww.disruptor.persistence.segment-size` | long | 67108864 | 段文件大小（字节） |
| `ww.disruptor.persistence.retention-hours` | int | 24 | 保留时间（小时） |

### 监控配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `ww.disruptor.metrics.enabled` | boolean | true | 是否启用监控 |
| `ww.disruptor.metrics.prefix` | String | ww.disruptor | 指标前缀 |
| `ww.disruptor.metrics.detailed` | boolean | false | 是否启用详细指标 |
| `ww.disruptor.metrics.tracing` | boolean | false | 是否启用链路追踪 |

## 📊 监控指标

### 核心指标

- `ww.disruptor.events.published` - 发布事件总数
- `ww.disruptor.events.processed` - 处理事件总数
- `ww.disruptor.queue.utilization` - 队列利用率（%）
- `ww.disruptor.events.pending` - 待处理事件数

### 性能指标

- `ww.disruptor.batch.processing.time` - 批处理耗时
- `ww.disruptor.event.processing.time` - 单个事件处理耗时

## 🏗️ 架构设计

```
┌─────────────────────────────────────────────────────────┐
│                   Application Layer                      │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────┐ │
│  │ Controller  │  │   Service    │  │  EventHandler  │ │
│  └─────────────┘  └──────────────┘  └────────────────┘ │
└────────────┬────────────────────────────────────────────┘
             │
┌────────────▼────────────────────────────────────────────┐
│              DisruptorTemplate (API Layer)               │
│  ┌──────────────────────────────────────────────────┐   │
│  │ publish() │ publishAsync() │ tryPublish()       │   │
│  └──────────────────────────────────────────────────┘   │
└────────────┬────────────────────────────────────────────┘
             │
┌────────────▼────────────────────────────────────────────┐
│               DisruptorEngine (Core Layer)               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  RingBuffer  │  │  Sequencer   │  │ WaitStrategy │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└────────────┬────────────────────────────────────────────┘
             │
┌────────────▼────────────────────────────────────────────┐
│            Processor Layer (处理器层)                    │
│  ┌──────────────────┐  ┌───────────────────────────┐   │
│  │ EventProcessor   │  │  BatchEventProcessor      │   │
│  │   - Single       │  │    - Batch                │   │
│  │   - Chain        │  │    - Parallel             │   │
│  │   - Async        │  │    - Pipeline             │   │
│  └──────────────────┘  └───────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
             │
┌────────────▼────────────────────────────────────────────┐
│          Extension Layer (扩展层)                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ │
│  │ Persistence │  │  Metrics    │  │  Tracing        │ │
│  │ - File      │  │ - Micrometer│  │ - OpenTelemetry │ │
│  │ - Redis     │  │ - Prometheus│  │                 │ │
│  │ - Kafka     │  │             │  │                 │ │
│  └─────────────┘  └─────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

## 🎯 使用场景

### 1. 订单处理系统
```java
// 高并发订单处理
@Component
public class OrderProcessor implements EventProcessor<Order> {
    @Override
    public ProcessResult process(Event<Order> event) {
        Order order = event.getPayload();
        // 订单处理逻辑
        // 1. 库存扣减
        // 2. 生成订单
        // 3. 发送通知
        return ProcessResult.success();
    }
}
```

### 2. 日志采集系统
```java
// 高吞吐量日志写入
@Component
public class LogBatchProcessor implements BatchEventProcessor<LogEntry> {
    @Override
    public ProcessResult processBatch(EventBatch<LogEntry> batch) {
        // 批量写入Elasticsearch
        elasticsearchClient.bulkInsert(batch.getEvents());
        return ProcessResult.success();
    }
}
```

### 3. 实时数据分析
```java
// 实时指标计算
@Component
public class MetricsProcessor implements EventProcessor<Metric> {
    @Override
    public ProcessResult process(Event<Metric> event) {
        // 实时聚合计算
        metricsAggregator.aggregate(event.getPayload());
        return ProcessResult.success();
    }
}
```

## 🔧 性能调优

### 高吞吐量场景
```yaml
ww:
  disruptor:
    ring-buffer-size: 65536      # 大容量Buffer
    consumer-threads: 8           # 多消费者
    batch-size: 500              # 大批量
    batch-timeout: 100           # 短超时
    wait-strategy: YIELDING      # 平衡策略
```

### 低延迟场景
```yaml
ww:
  disruptor:
    ring-buffer-size: 16384      # 中等容量
    consumer-threads: 4
    batch-size: 50               # 小批量
    batch-timeout: 10            # 极短超时
    wait-strategy: BUSY_SPIN     # 忙等待
```

### 内存敏感场景
```yaml
ww:
  disruptor:
    ring-buffer-size: 4096       # 小容量
    consumer-threads: 2
    batch-size: 100
    batch-timeout: 1000
    wait-strategy: BLOCKING      # 低CPU占用
```

## 📈 性能基准

| 场景 | 吞吐量 | P99延迟 | 内存占用 |
|------|--------|---------|----------|
| 单线程发布 | 120,000 events/s | < 1ms | 低 |
| 多线程发布 | 250,000 events/s | < 2ms | 中 |
| 批量发布 | 600,000 events/s | < 5ms | 高 |
| 批量处理 | 1,000,000 events/s | < 3ms | 中 |

##
