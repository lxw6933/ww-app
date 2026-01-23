# WW Disruptor Starter 项目结构

## 📁 项目概述

这是一个**全新重构**的基于LMAX Disruptor的高性能事件处理Spring Boot Starter模块，采用**模块化、高扩展性、易用**的架构设计。

## 🏗️ 目录结构

```
ww-spring-boot-starter-disruptor/
├── src/
│   ├── main/
│   │   ├── java/com/ww/app/disruptor/
│   │   │   ├── model/                          # 数据模型层
│   │   │   │   ├── Event.java                  # 事件基类（支持泛型）
│   │   │   │   ├── EventStatus.java            # 事件状态枚举
│   │   │   │   ├── EventMetadata.java          # 事件元数据
│   │   │   │   ├── EventPriority.java          # 事件优先级枚举
│   │   │   │   ├── EventBatch.java             # 事件批次
│   │   │   │   ├── BatchStatus.java            # 批次状态枚举
│   │   │   │   └── ProcessResult.java          # 处理结果封装
│   │   │   │
│   │   │   ├── processor/                      # 处理器层（可插拔）
│   │   │   │   ├── EventProcessor.java         # 单事件处理器接口
│   │   │   │   └── BatchEventProcessor.java    # 批量事件处理器接口
│   │   │   │
│   │   │   ├── core/                           # 核心引擎层
│   │   │   │   ├── DisruptorEngine.java        # Disruptor引擎核心
│   │   │   │   └── DisruptorConfig.java        # 引擎配置类
│   │   │   │
│   │   │   ├── api/                            # API层（对外接口）
│   │   │   │   └── DisruptorTemplate.java      # 模板类（类似RestTemplate）
│   │   │   │
│   │   │   └── config/                         # 配置层
│   │   │       ├── DisruptorProperties.java    # 配置属性
│   │   │
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   └── spring/
│   │       └── application-example.yml         # 示例配置文件
│   │
│   └── test/
│       └── java/com/ww/app/disruptor/
│           └── DisruptorTemplateTest.java      # 单元测试
│
├── pom.xml                                     # Maven依赖配置
├── README.md                                   # 项目说明文档
└── PROJECT_STRUCTURE.md                        # 本文件

```

## 🎨 架构设计

### 分层架构

```
┌─────────────────────────────────────┐
│      Application Layer              │  应用层
│   (用户业务代码)                     │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      API Layer                      │  API层
│   DisruptorTemplate                 │  (易用的模板类)
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      Core Layer                     │  核心层
│   DisruptorEngine                   │  (Disruptor引擎)
│   DisruptorConfig                   │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      Processor Layer                │  处理器层
│   EventProcessor                    │  (可插拔处理器)
│   BatchEventProcessor               │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      Model Layer                    │  模型层
│   Event, EventBatch                 │  (数据模型)
│   ProcessResult                     │
└─────────────────────────────────────┘
```

### 核心组件说明

#### 1. 模型层 (model/)

**职责**: 定义核心数据结构

- **Event**: 事件基类，支持泛型，包含事件ID、类型、负载、元数据等
- **EventMetadata**: 事件元数据，支持链路追踪、优先级、分区键等
- **EventBatch**: 批量事件容器，用于批处理
- **ProcessResult**: 处理结果封装，统一的返回格式

**设计亮点**:
- 泛型支持，类型安全
- 丰富的元数据支持（链路追踪、优先级）
- 状态机设计（CREATED → PROCESSING → COMPLETED/FAILED）

#### 2. 处理器层 (processor/)

**职责**: 定义事件处理接口

- **EventProcessor**: 单事件处理器接口（函数式接口）
- **BatchEventProcessor**: 批量事件处理器接口

**设计亮点**:
- 函数式接口，支持Lambda表达式
- 支持事件类型过滤（supports方法）
- 支持处理器优先级
- 批量处理器可自定义批量大小和超时

#### 3. 核心层 (core/)

**职责**: Disruptor引擎实现

- **DisruptorEngine**: 引擎核心，管理RingBuffer、线程池、事件分发
- **DisruptorConfig**: 引擎配置，包括RingBuffer大小、线程数、等待策略等

**设计亮点**:
- 多种等待策略支持（BLOCKING、YIELDING、SLEEPING、BUSY_SPIN）
- 批量处理优化（批量缓冲 + 定时刷新）
- 完善的生命周期管理（start/stop）
- 实时监控指标（发布计数、处理计数、队列利用率）

#### 4. API层 (api/)

**职责**: 提供易用的API

- **DisruptorTemplate**: 模板类，类似RestTemplate的设计

**设计亮点**:
- Builder模式，流式API
- 多种发布方式（同步、异步、非阻塞）
- 简化的API（publish(eventType, payload)）
- 丰富的监控方法

#### 5. 配置层 (config/)

**职责**: 配置属性定义（业务自建时可选使用）

- **DisruptorProperties**: 配置属性绑定


## 🚀 核心特性

### 1. 高性能

- **零拷贝**: 使用RingBuffer预分配，避免GC
- **批量处理**: 减少上下文切换
- **多种等待策略**: 适应不同场景
- **性能目标**: > 1,000,000 events/s

### 2. 易用性

```java
import com.ww.app.common.utils.ThreadUtil;

// Builder构建
com.ww.app.disruptor.api.DisruptorTemplate<String> template = com.ww.app.disruptor.api.DisruptorTemplate.<String>builder()
        .ringBufferSize(1024)
        .batchBufferCapacity(10000)
        .batchBufferOverflow("DROP")
        .executor(ThreadUtil.initFixedThreadPoolExecutor("xxx", 8))
        .eventProcessor(event -> ProcessResult.success())
        .build();
```

### 3. 高扩展性

- **处理器可插拔**: 实现EventProcessor接口即可
- **SPI扩展**: 持久化、监控均支持扩展
- **策略模式**: 等待策略、重试策略可配置

### 4. 模块化设计

每个模块职责清晰，低耦合高内聚：
- Model层：纯数据模型，无依赖
- Processor层：只依赖Model层
- Core层：核心实现，依赖Processor和Model
- API层：封装Core层，提供易用接口
- Config层：配置属性定义（业务自建可选）

## 📊 与旧版本对比

| 特性 | 旧版本 | 新版本 |
|------|--------|--------|
| 包路径 | com.ww.app.disruptor | com.ww.app.disruptor |
| 模块化 | 较弱 | 强（5层架构） |
| 扩展性 | 中等 | 高（SPI + 插件化） |
| 易用性 | 一般 | 优秀（Template + Builder） |
| 文档 | 基础 | 完善（README + 示例） |
| 测试 | 有 | 完善（单元测试 + 示例） |

## 🎯 使用场景

### 1. 高并发订单处理
```java
@Component
public class OrderProcessor implements EventProcessor<Order> {
    @Override
    public ProcessResult process(Event<Order> event) {
        // 处理订单
        return ProcessResult.success();
    }
}
```

### 2. 批量日志写入
```java
@Component
public class LogBatchProcessor implements BatchEventProcessor<LogEntry> {
    @Override
    public ProcessResult processBatch(EventBatch<LogEntry> batch) {
        // 批量写入
        return ProcessResult.success();
    }
}
```

### 3. 实时数据分析
```java
template.publish("metric", metricData);
```

## 🔧 配置说明

### 关键配置项

```yaml
ww:
  disruptor:
    ring-buffer-size: 1024      # RingBuffer大小（2的幂）
    consumer-threads: 4          # 消费者线程数
    batch-size: 100              # 批处理大小
    batch-buffer-capacity: 10000 # 批量缓冲区最大容量
    batch-buffer-overflow: DROP  # 批量缓冲区溢出策略（DROP/BLOCK）
    wait-strategy: BLOCKING      # 等待策略
```

### 性能调优建议

- **高吞吐量**: ring-buffer-size=65536, wait-strategy=YIELDING
- **低延迟**: ring-buffer-size=16384, wait-strategy=BUSY_SPIN
- **低资源**: ring-buffer-size=4096, wait-strategy=BLOCKING

## 📈 未来规划

### 短期（已实现）
- ✅ 核心引擎重构
- ✅ 模块化架构
- ✅ Builder模式API
- ✅ 完善文档

### 中期（计划中）
- ⏳ 注解驱动（@DisruptorEventHandler）
- ⏳ 持久化模块（File、Redis、Kafka）
- ⏳ 监控模块（Micrometer集成）
- ⏳ 链路追踪（OpenTelemetry）

### 长期（规划中）
- ⏳ 可视化监控面板
- ⏳ 动态扩容
- ⏳ 多RingBuffer支持
- ⏳ 事件回放功能

## 📝 开发指南

### 添加新的处理器

1. 实现EventProcessor或BatchEventProcessor接口
2. 添加@Component注解
3. 业务自建时自行注册或注入

### 扩展持久化

1. 创建PersistenceProvider接口（计划中）
2. 实现接口方法
3. 通过配置指定类型

### 自定义等待策略

修改DisruptorEngine.createWaitStrategy()方法

## 📖 参考资料

- [LMAX Disruptor](https://github.com/LMAX-Exchange/disruptor)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [项目README](./README.md)

## 👥 贡献者

- **ww-framework Team**

## 📄 许可证

MIT License

---

**注**: 本项目为全新重构版本，采用现代化的架构设计，为生产环境提供高性能、易用、可扩展的事件处理解决方案。
