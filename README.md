# WW App 微服务系统

面向电商、即时通讯、开放平台与后台管理的一体化微服务解决方案，基于 Spring Boot + Spring Cloud Alibaba 构建，强调模块化、可扩展与高可用。

## 架构概览
系统采用微服务架构，主要分为：
- **入口层**：`ww-api-gateway` 统一路由、鉴权与限流。
- **认证层**：`ww-auth` 提供统一认证与授权能力。
- **业务层**：电商、IM、开放平台、后台管理等服务按域拆分。
- **基础能力**：`ww-framework` 提供统一 Starter 与通用组件。
- **基础设施**：Nacos、MySQL、Redis、MQ、Elasticsearch、MongoDB 等。

## 模块职责（服务视角）
| 模块 | 主要职责 | 说明/子模块 |
| --- | --- | --- |
| ww-api-gateway | 统一入口、路由、限流 | Spring Cloud Gateway + Sentinel |
| ww-auth | 认证授权中心 | OAuth2/JWT |
| ww-admin-manage | 后台管理系统 | 运营与配置管理 |
| ww-open-platform | 开放平台 | 应用管理、API管理、权限与签名 |
| ww-im | 即时通讯 | Netty + Disruptor，支持单聊/群聊/红包 |
| ww-mall | 电商主链路 | `ww-product`、`ww-order`、`ww-pay`、`ww-cart`、`ww-coupon`、`ww-search`、`ww-seckill`、`ww-lottery`、`ww-promotion` |
| ww-consumer | 消费者服务 | 异步任务与消息消费 |
| ww-flink | 实时分析 | 订单实时分析（Kafka + Flink） |
| ww-grpc | 服务间通信 | gRPC 接口与实现 |
| ww-framework | 通用基础能力 | Web/Security/Redis/MQ/监控等 Starter |
| ww-dependencies | 依赖版本管理 | 统一 BOM 与版本对齐 |
| ww-third-server | 第三方集成 | 支付/短信/外部平台 |

## 快速启动
### 1) 准备环境
- JDK 1.8+
- Maven 3.6+
- Nacos 2.x
- MySQL 8、Redis 6
- Kafka 2.8（如启用 Flink 分析）
- MongoDB（如启用 IM）
- RabbitMQ（如启用 IM / 促销等异步场景）

### 2) 初始化数据库
SQL 位于 `script/db`：
```bash
mysql -h127.0.0.1 -uroot -p < script/db/mall-admin.sql
mysql -h127.0.0.1 -uroot -p < script/db/mall-member.sql
mysql -h127.0.0.1 -uroot -p < script/db/mall-product.sql
```

### 3) 导入 Nacos 配置（推荐）
配置包位于 `script/nacos/nacos_config_export_*.zip`，在 Nacos 控制台导入即可。

### 4) 编译打包
```bash
# 全量编译
mvn clean package -DskipTests

# 单模块编译（含依赖）
mvn clean package -pl ww-api-gateway -am -DskipTests
```

### 5) 本地启动示例
```bash
# 以网关为例
mvn -pl ww-api-gateway spring-boot:run

# 或运行打包后的 jar
java -jar ww-api-gateway/target/ww-api-gateway.jar
```

建议启动顺序：Nacos → Gateway → Auth → 其他业务服务。

## 部署说明
项目内提供基础 `Dockerfile`（使用 `my.jar` 作为应用包）：
```bash
mvn -pl ww-api-gateway -am -DskipTests package
copy ww-api-gateway\\target\\ww-api-gateway.jar my.jar
docker build -t ww-api-gateway:local .
docker run -p 19001:19001 ww-api-gateway:local
```
如需部署其它服务，替换 `my.jar` 为对应模块打包产物即可。

## 开发与规范
- Java 命名：类 `UpperCamelCase`，方法/变量 `lowerCamelCase`，常量 `UPPER_SNAKE_CASE`
- 接口设计：RESTful 风格，统一返回结构
- 提交规范：Angular 提交格式 `feat(模块): 简短描述`
- 代码风格与更多规则参见 `AGENTS.md`

## 贡献指南
1. Fork 仓库并创建特性分支
2. 本地开发与自测
3. 提交并发起 PR（附变更说明与测试信息）

## 许可证
本项目采用 MIT License，详见 `LICENSE`。
