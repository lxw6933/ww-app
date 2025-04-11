# 实时订单分析系统

本项目是一个基于Spring Boot和Apache Flink的实时订单分析系统，用于模拟电商平台的订单数据处理和分析。

## 功能特点

- 模拟生成真实的电商订单数据流
- 订单状态实时跟踪和分析
- 基于Flink的实时数据处理
- 异常交易检测
- 多维度数据分析（地区、渠道、商品分类等）
- 实时销售统计和监控

## 系统架构

系统由以下主要组件构成：

1. **订单事件生成器**：模拟产生电商订单事件流，包括订单创建、支付、发货、收货、完成等状态
2. **Kafka消息队列**：存储和传输订单事件数据
3. **Flink处理引擎**：从Kafka读取数据并进行实时处理和分析
4. **订单分析模块**：包含多个实时分析任务

## 配置说明

在`application.yml`中配置系统参数：

```yaml
ww:
  flink:
    job:
      order:
        generator:
          enable: true     # 是否启用订单生成器
          rate: 100        # 每秒生成事件数（估计值）
        enable: true       # 是否启用订单分析作业
        topic: order_events # Kafka主题
        bootstrap-servers: localhost:9092 # Kafka地址
```

## 快速开始

### 1. 准备环境

确保安装了以下软件：

- Java 8+
- Maven 3.6+
- Kafka 2.8+

### 2. 启动Kafka

如果未安装Kafka，可以使用Docker启动：

```bash
docker run -d --name kafka -p 9092:9092 -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092 -e KAFKA_CREATE_TOPICS="order_events:1:1" wurstmeister/kafka
```

### 3. 启动应用

```bash
mvn spring-boot:run
```

应用启动后，将自动开始生成订单数据并进行处理。

## 分析模块介绍

系统包含以下分析模块：

1. **支付类型分析**：按支付方式统计订单数量和金额
2. **异常交易检测**：识别异常大额或小额交易
3. **商品分类排名**：商品分类销售量实时排行
4. **订单转化率统计**：计算订单从创建到完成的转化率
5. **地区订单分布**：按地区统计订单分布情况
6. **渠道效果分析**：分析不同渠道的订单效果
7. **实时交易金额监控**：实时统计交易总额

## 开发指南

### 添加新的分析模块

要添加新的分析模块，可以按照以下步骤操作：

1. 在`OrderAnalyticsJob`类中添加新的数据流处理逻辑
2. 创建相应的窗口或处理函数
3. 配置输出目标（如控制台、数据库、消息队列等）

示例：

```java
// 添加新的分析模块
orderEventStream
    .keyBy(OrderEvent::getSomeKey)
    .window(TumblingProcessingTimeWindows.of(Time.minutes(5)))
    .process(new YourCustomWindowFunction())
    .name("Your Analysis Name")
    .print();
```

### 自定义订单生成逻辑

可以通过修改`OrderEventGenerator`类来自定义订单生成逻辑，例如：

- 调整生成速率
- 修改商品分类、支付方式、地区等数据分布
- 添加更多订单属性

## 性能调优

对于生产环境，建议进行以下调优：

1. 增加Flink任务并行度（`ww.flink.parallelism`）
2. 配置适当的检查点间隔（`ww.flink.checkpoint-interval`）
3. 使用更稳定的状态后端（如RocksDB）
4. 调整Kafka分区数以提高吞吐量

## 监控和管理

可以通过以下方式监控系统：

1. 应用日志中的实时分析结果
2. Flink Web UI（默认 http://localhost:8081）
3. 通过JMX监控JVM指标

## 代码示例

### 异常交易检测示例

```java
public static class AnomalyDetectionFunction extends KeyedProcessFunction<Long, OrderEvent, String> {
    private transient ValueState<BigDecimal> userAvgAmountState;

    @Override
    public void open(Configuration parameters) throws Exception {
        ValueStateDescriptor<BigDecimal> descriptor = new ValueStateDescriptor<>(
                "user-avg-amount",
                BigDecimal.class
        );
        userAvgAmountState = getRuntimeContext().getState(descriptor);
    }

    @Override
    public void processElement(OrderEvent event, Context ctx, Collector<String> out) throws Exception {
        BigDecimal currentAvg = userAvgAmountState.value();
        if (currentAvg == null) {
            userAvgAmountState.update(event.getAmount());
            return;
        }

        // 计算新的平均值
        BigDecimal newAvg = currentAvg.add(event.getAmount())
            .divide(BigDecimal.valueOf(2), 2, BigDecimal.ROUND_HALF_UP);
        userAvgAmountState.update(newAvg);

        // 检测异常
        BigDecimal ratio = event.getAmount().divide(currentAvg, 2, BigDecimal.ROUND_HALF_UP);
        if (ratio.compareTo(BigDecimal.valueOf(3)) > 0) {
            out.collect(String.format(
                    "异常大额交易警报 - 用户ID: %d, 订单ID: %s, 金额: %.2f, 平均金额: %.2f, 比率: %.2f",
                    event.getUserId(),
                    event.getOrderId(),
                    event.getAmount(),
                    currentAvg,
                    ratio
            ));
        }
    }
}
``` 