# WW App 微服务系统

面向电商、即时通讯、开放平台与后台管理的一体化微服务解决方案，基于 Spring Boot + Spring Cloud 构建，强调模块化、可扩展与高可用。

## 项目概览
- **架构风格**：Spring Cloud Alibaba 微服务架构
- **核心能力**：电商业务、IM 即时通讯、开放平台、后台管理
- **运行形态**：多模块 Maven 聚合项目，服务独立部署

## 目录结构
```
├── ww-admin-manage     # 后台管理系统
├── ww-api-gateway      # API 网关
├── ww-auth             # 认证授权
├── ww-consumer         # 消费者服务
├── ww-dependencies     # 依赖版本管理
├── ww-flink            # Flink 大数据处理
├── ww-framework        # 通用框架与 Starter
├── ww-grpc             # gRPC 通信
├── ww-im               # 即时通讯
├── ww-mall             # 电商系统（购物车/订单/支付/商品等）
├── ww-open-platform    # 开放平台
├── ww-task             # 任务调度
└── ww-third-server     # 第三方服务集成
```

## 技术栈
- **基础框架**：Spring Boot 2.7.x、Spring Cloud 2021、Spring Cloud Alibaba
- **注册配置**：Nacos / Nacos Config
- **网关与限流**：Spring Cloud Gateway / Sentinel
- **数据与中间件**：MySQL、Redis、Elasticsearch、Kafka、RabbitMQ、MinIO
- **可观测性**：Prometheus、Grafana、SkyWalking、ELK
- **任务调度**：XXL-Job、Flink

## 快速开始
### 1. 克隆项目
```bash
git clone https://gitee.com/ww6933/ww-app.git
```

### 2. 初始化数据库
```bash
cd script/sql
mysql -h127.0.0.1 -uroot -p < init.sql
```

### 3. 编译打包
```bash
# 全量编译
mvn clean package -DskipTests

# 单模块编译（含依赖）
mvn clean package -pl ww-api-gateway -am -DskipTests
```

### 4. 启动服务
建议启动顺序：
1. Nacos（配置中心/注册中心）
2. `ww-api-gateway`
3. 认证、基础服务（如 `ww-auth`、`ww-framework`）
4. 其他业务服务

```bash
java -jar ww-api-gateway/target/ww-api-gateway.jar
```

## 模块说明（精选）
## 模块清单（服务视角）
| 模块 | 主要职责 | 示例子模块/说明 |
| --- | --- | --- |
| ww-api-gateway | 统一入口、路由、鉴权、限流 | Spring Cloud Gateway + Sentinel |
| ww-auth | 认证授权中心 | OAuth2/JWT、统一登录 |
| ww-admin-manage | 后台管理系统 | 用户/权限/配置/运营 |
| ww-open-platform | 开放平台能力 | 开发者管理、应用管理、开放接口 |
| ww-im | 即时通讯 | 私聊/群聊/在线状态/历史消息 |
| ww-mall | 电商业务主线 | `ww-product`、`ww-order`、`ww-pay`、`ww-cart`、`ww-coupon`、`ww-search`、`ww-seckill`、`ww-lottery` |
| ww-consumer | 消费者服务 | 消息消费与异步任务处理 |
| ww-flink | 实时数据处理 | 实时分析、推荐/画像 |
| ww-grpc | 高性能服务调用 | gRPC 接口定义与服务实现 |
| ww-framework | 通用基础能力 | Web/Security/Redis/MQ/监控等 Starter |
| ww-dependencies | 依赖版本管理 | 统一 BOM 与版本对齐 |
| ww-third-server | 第三方集成 | 支付/短信/外部平台对接 |
| ww-task | 任务调度 | 定时/延时/分布式任务 |

### ww-framework
统一的基础能力与 Starter 集合，涵盖：
- Redis / MQ / 搜索 / Web / 安全 / 监控 / WebSocket
- 常用工具、注解、AOP、通用异常与上下文管理

### ww-mall
完整电商服务集合，包含：
`ww-product`、`ww-order`、`ww-pay`、`ww-cart`、`ww-coupon`、`ww-search`、`ww-seckill` 等。

### ww-im
即时通讯能力，支持聊天、群组、在线状态、历史记录等功能。

## 运行环境
- **JDK**：1.8+
- **Maven**：3.6+
- **Docker**：20.10+（推荐）
- **Node.js**：14+（如涉及前端模块）
- **中间件**：MySQL 8、Redis 6、Kafka 2.8、Elasticsearch 7、Nacos 2

## 开发规范
- Java 命名：类 `UpperCamelCase`，方法/变量 `lowerCamelCase`，常量 `UPPER_SNAKE_CASE`
- 接口设计：RESTful 风格，统一返回结构
- 提交规范：Angular 提交格式  
  `feat(模块): 简短描述`

## 文档与扩展
- 架构图：`docs/images/architecture.png`
- 开发规范：`docs/development-guide.md`
- 分布式事务：`docs/distributed-transaction.md`
- 监控配置：`docs/monitoring.md`

## 贡献方式
1. Fork 仓库并创建特性分支
2. 本地开发与测试
3. 提交并发起 PR

## 许可证
本项目采用 MIT License，详见 `LICENSE`。
