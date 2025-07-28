<p align="center">
 <img src="https://img.shields.io/badge/Spring%20Cloud-2021-blue.svg" alt="Spring Cloud">
 <img src="https://img.shields.io/badge/Spring%20Boot-2.7.18-blue.svg" alt="Spring Boot">
 <img src="https://img.shields.io/badge/Java-1.8-blue.svg" alt="Java">
 <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License">
</p>

# WW App 微服务系统

## 项目概述

WW App 是一个基于微服务架构设计的现代综合系统，提供完整的电商业务功能、即时通讯、开放平台、后台管理等多种功能，是一个全面的企业级应用解决方案。

## 项目架构

项目采用微服务架构，将不同的业务功能拆分为独立的服务模块，实现高内聚低耦合的系统设计。

```
├── ww-admin-manage     # 后台管理系统
├── ww-api-gateway      # API网关服务
├── ww-auth             # 认证授权服务
├── ww-consumer         # 消费者服务
├── ww-dependencies     # 依赖管理
├── ww-flink            # Flink大数据处理
├── ww-framework        # 通用框架
├── ww-grpc             # gRPC通信服务
├── ww-im               # 即时通讯服务
├── ww-mall             # 电商系统
│   ├── ww-cart         # 购物车服务
│   ├── ww-coupon       # 优惠券服务
│   ├── ww-lottery      # 抽奖服务
│   ├── ww-member       # 会员服务
│   ├── ww-order        # 订单服务
│   ├── ww-pay          # 支付服务
│   ├── ww-product      # 商品服务
│   ├── ww-search       # 搜索服务
│   └── ww-seckill      # 秒杀服务
├── ww-open-platform    # 开放平台
├── ww-task             # 任务调度服务
└── ww-third-server     # 第三方服务集成
```

## 技术栈

### 基础框架
- **微服务框架**：Spring Boot、Spring Cloud
- **网关**：Spring Cloud Gateway
- **服务注册与发现**：Nacos
- **配置中心**：Nacos Config
- **服务调用**：OpenFeign、gRPC
- **容器化**：Docker、Kubernetes

### 数据存储与处理
- **关系型数据库**：MySQL
- **缓存**：Redis
- **搜索引擎**：Elasticsearch
- **消息队列**：RabbitMQ、Kafka
- **大数据处理**：Flink、Spark
- **对象存储**：MinIO

### 开发工具与框架
- **ORM框架**：MyBatis-Plus
- **API文档**：Swagger、Knife4j
- **安全框架**：Spring Security、OAuth2
- **分布式事务**：Seata
- **分布式锁**：Redisson
- **任务调度**：XXL-Job
- **监控**：Prometheus、Grafana
- **日志**：ELK Stack (Elasticsearch, Logstash, Kibana)

### 前端技术
- **前端框架**：Vue.js、React
- **UI组件库**：Element UI、Ant Design
- **移动端**：微信小程序、Flutter

## 模块说明

### 核心基础服务

#### ww-framework

通用框架模块，提供各种Spring Boot Starter组件，简化各个微服务的开发。包含以下子模块：

```
ww-framework
├── ww-common                         # 通用工具类、常量、枚举、异常处理等
    ├── annotation                    # 自定义注解
    ├── common                        # 通用基础类
    ├── constant                      # 系统常量定义
    ├── context                       # 上下文相关实现
    ├── enums                         # 枚举类型定义
    ├── exception                     # 异常处理机制
    ├── interfaces                    # 通用接口定义
    ├── queue                         # 队列相关实现
    ├── serializer                    # 序列化/反序列化工具
    ├── thread                        # 线程池和线程管理
    ├── utils                         # 各类工具方法集合
    └── valid                         # 数据验证相关功能
├── ww-spring-boot-starter-es         # Elasticsearch集成，提供搜索引擎功能
├── ww-spring-boot-starter-excel      # Excel导入导出功能
├── ww-spring-boot-starter-influxdb   # InfluxDB时序数据库集成，用于存储监控数据
├── ww-spring-boot-starter-ip         # IP地址解析、地理位置查询等功能
├── ww-spring-boot-starter-job        # 分布式任务调度功能
├── ww-spring-boot-starter-kafka      # Kafka消息队列集成
├── ww-spring-boot-starter-minio      # MinIO对象存储集成，用于文件存储
├── ww-spring-boot-starter-mongodb    # MongoDB集成，用于非关系型数据存储
├── ww-spring-boot-starter-monitor    # 系统监控功能，包括健康检查、指标收集等
├── ww-spring-boot-starter-mybatis    # MyBatis集成，提供ORM和数据访问功能
├── ww-spring-boot-starter-rabbitmq   # RabbitMQ消息队列集成
├── ww-spring-boot-starter-redis      # Redis缓存集成，提供缓存、分布式锁等功能
    ├── annotation                    # 自定义注解（分布式锁、限流、消息发布、防重复提交等）
    ├── aspect                        # 切面实现（处理各类注解的业务逻辑）
    ├── codec                         # Redis编解码器
    ├── component                     # Redis组件
    ├── config                        # Redis配置类
    ├── controller                    # 控制器
    ├── key                           # Redis键管理
    ├── listener                      # Redis事件监听器
    ├── service                       # Redis服务实现
    ├── vo                            # 值对象
    └── resources/lua                 # Redis Lua脚本（库存管理、限流等）
├── ww-spring-boot-starter-security   # 安全框架集成，提供认证、授权等功能
├── ww-spring-boot-starter-sensitive  # 敏感数据处理，包括脱敏、加密等功能
├── ww-spring-boot-starter-web        # Web应用集成，提供统一的Web开发体验
    ├── annotation                    # 自定义注解（如API安全注解、XSS防护注解）
    ├── aop                           # 面向切面编程（API安全切面、控制器切面）
    ├── api                           # API示例实现
    ├── config                        # 配置类（文档、启动器、哨兵、系统、线程池等）
    ├── filter                        # 过滤器实现
    ├── handler                       # 处理器（请求/响应处理、异常处理）
    ├── holder                        # 上下文持有者
    ├── interceptor                   # 拦截器（Feign请求拦截、gRPC拦截）
    ├── properties                    # 属性配置类
    ├── utils                         # Web相关工具类
    └── validator                     # 数据验证器
└── ww-spring-boot-starter-websocket  # WebSocket集成，提供实时通信功能
```

#### ww-api-gateway

API网关服务，统一管理服务入口，提供路由转发、负载均衡、权限验证、限流熔断等功能。

#### ww-auth

认证授权服务，提供统一的用户认证、授权管理、权限控制等安全相关功能。

#### ww-dependencies

依赖管理模块，统一管理项目依赖版本，确保各模块依赖的一致性和兼容性。

#### ww-task

任务调度服务，提供定时任务、延时任务、分布式任务的调度和执行功能。

### 业务支撑服务

#### ww-admin-manage

后台管理系统，提供系统配置、用户管理、权限管理、数据统计等后台运营功能。

#### ww-consumer

消费者服务，处理消息队列中的消息，执行异步任务处理。

#### ww-flink

Flink大数据处理模块，提供实时数据分析、用户行为分析、推荐系统等大数据处理功能。

#### ww-grpc

gRPC通信服务，提供高性能的服务间通信功能，支持跨语言调用。

#### ww-im

即时通讯服务，提供实时消息、聊天室、客服系统等即时通讯功能。

#### ww-open-platform

开放平台，提供API开放接口、开发者管理、应用管理、文档中心等功能。

#### ww-third-server

第三方服务集成，提供与第三方平台（如支付宝、微信、短信服务等）的集成功能。

### 电商业务服务 (ww-mall)

#### ww-product

商品服务，负责商品信息的管理，包括商品分类、品牌、属性、规格、库存等。

#### ww-order

订单服务，处理订单创建、支付、发货、退款等订单全生命周期管理。

#### ww-member

会员服务，管理用户注册、登录、个人信息、会员等级等功能。

#### ww-cart

购物车服务，提供商品加入购物车、修改数量、选择结算等功能。

#### ww-pay

支付服务，集成多种支付方式，处理支付流程和回调。

#### ww-coupon

优惠券服务，管理优惠券的创建、发放、使用和验证。

#### ww-search

搜索服务，基于Elasticsearch实现商品搜索、筛选、排序等功能。

#### ww-seckill

秒杀服务，处理高并发的秒杀活动，包括库存预热、限流、防刷等。

#### ww-lottery

抽奖服务，提供营销活动中的抽奖功能。

## 环境要求

### 开发环境
- JDK 1.8+
- Maven 3.6+
- Docker 20.10+
- Docker Compose 2.0+
- Node.js 14+
- npm 6+ 或 Yarn 1.22+

### 中间件
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.8+
- Kafka 2.8+
- Elasticsearch 7.x
- Nacos 2.x
- MinIO

## 快速开始

### 1. 克隆项目

```bash
git clone https://gitee.com/ww6933/ww-app.git
```


### 2. 初始化数据库

```bash
cd script/sql
# 根据实际情况修改数据库连接信息
mysql -h127.0.0.1 -uroot -p < init.sql
```

### 3. 编译打包

```bash
# 编译整个项目
cd ww-app
mvn clean package -DskipTests

# 或者编译单个模块
mvn clean package -pl ww-api-gateway -am -DskipTests
```

### 4. 启动服务

按照以下顺序启动各个服务：

1. 启动Nacos注册中心（如果使用Docker Compose，已经启动）
2. 启动ww-api-gateway网关服务
3. 启动核心基础服务（ww-auth等）
4. 启动其他业务服务模块

```bash
# 示例：启动网关服务
java -jar ww-api-gateway/target/ww-api-gateway.jar
```

## 项目特性

- **微服务架构**：基于Spring Cloud Alibaba的微服务架构，服务独立部署，高度解耦
  - 服务注册与发现：使用Nacos实现服务的自动注册与发现
  - 配置中心：集中管理各服务配置，支持动态配置更新
  - 服务网关：基于Spring Cloud Gateway实现API路由、认证、限流等功能

- **高可用设计**：完善的服务治理机制，保障系统稳定性
  - 负载均衡：基于Ribbon的客户端负载均衡
  - 熔断降级：使用Sentinel实现服务熔断、限流和降级
  - 服务容错：自动重试、超时控制、故障转移等机制

- **分布式事务**：基于Seata的分布式事务处理，确保数据一致性
  - AT模式：无侵入的分布式事务解决方案
  - TCC模式：支持复杂业务场景的两阶段提交
  - Saga模式：长事务支持，适合复杂业务流程

- **高性能**：多层次的性能优化策略
  - 缓存体系：多级缓存设计，本地缓存+分布式缓存
  - 异步处理：基于消息队列的异步任务处理
  - 读写分离：主从数据库分离，提升读取性能
  - 数据分片：按业务特性进行数据分片，提高数据处理能力

- **安全防护**：完善的认证授权机制，防止常见的Web安全漏洞
  - 统一认证：基于OAuth2.0和JWT的认证中心
  - 细粒度授权：RBAC权限模型，支持API级别的权限控制
  - 安全防护：防XSS、CSRF、SQL注入等常见攻击
  - 数据加密：敏感数据传输和存储加密

- **可观测性**：全方位的监控和诊断能力
  - 分布式日志：ELK日志收集分析平台
  - 链路追踪：使用SkyWalking实现分布式链路追踪
  - 系统监控：基于Prometheus和Grafana的监控告警体系
  - 健康检查：服务健康状态实时监测

- **开放平台**：标准化API接口，支持第三方应用接入
  - API网关：统一的API管理和访问控制
  - 开发者门户：自助式应用接入和管理
  - SDK支持：多语言SDK，简化接入流程
  - 沙箱环境：提供测试环境，便于开发调试

- **即时通讯**：支持实时消息推送，提供完整的IM解决方案
  - 私聊/群聊：支持一对一和多人群组聊天
  - 消息推送：多端消息实时推送
  - 在线状态：用户在线状态实时更新
  - 历史记录：消息历史记录存储和查询

- **大数据分析**：基于Flink的实时数据分析，支持用户行为分析和个性化推荐
  - 用户画像：基于用户行为构建用户画像
  - 实时分析：实时处理用户行为数据
  - 个性化推荐：基于用户兴趣的商品推荐
  - 营销策略：数据驱动的精准营销

## 系统架构图

![系统架构图](docs/images/architecture.png)

系统采用分层架构设计：
1. **基础设施层**：包括各种中间件和基础服务，如MySQL、Redis、Elasticsearch、Nacos等
2. **服务治理层**：提供服务注册发现、配置管理、负载均衡、熔断降级等能力
3. **核心服务层**：包含各个微服务模块，如商品、订单、会员等核心业务服务
4. **API网关层**：统一的API入口，处理路由转发、认证鉴权、限流熔断等
5. **应用层**：各种前端应用，包括Web管理后台、移动端App、小程序等

*注：如果图片无法显示，请查看 `docs/images` 目录下的架构图文件*

## 开发规范

项目遵循以下开发规范：

### 代码规范

- **Java规范**：遵循阿里巴巴Java开发手册
  - 命名规范：类名采用UpperCamelCase，方法名/变量名采用lowerCamelCase，常量采用UPPER_SNAKE_CASE
  - 注释规范：类、字段、方法必须添加Javadoc注释
  - 异常处理：不允许捕获异常后不处理或打印堆栈后吞掉异常
  - 代码格式：使用统一的代码格式化配置

- **前端规范**：
  - Vue组件采用PascalCase命名
  - CSS采用BEM命名规范
  - JavaScript变量和函数采用camelCase命名
  - 组件属性顺序：props, data, computed, watch, lifecycle methods, methods

### 提交规范

采用Angular提交规范，提交信息格式如下：

```
<type>(<scope>): <subject>

<body>

<footer>
```

- **type**: feat(新功能), fix(修复), docs(文档), style(格式), refactor(重构), perf(性能), test(测试), chore(构建/工具)
- **scope**: 影响范围，如模块名称
- **subject**: 简短描述，不超过50个字符
- **body**: 详细描述
- **footer**: 关闭Issue或Breaking Changes说明

### 分支管理

采用Git Flow工作流：

- **master**: 生产环境分支，只接受来自release和hotfix的合并
- **develop**: 开发主分支，包含最新的开发代码
- **feature/xxx**: 功能分支，从develop创建，完成后合并回develop
- **release/x.y.z**: 发布分支，从develop创建，完成后合并到master和develop
- **hotfix/xxx**: 热修复分支，从master创建，完成后合并到master和develop

### 接口设计

遵循RESTful API设计规范：

- 使用HTTP方法表示操作类型：GET(查询), POST(创建), PUT(更新), DELETE(删除)
- URL使用名词复数形式，如`/api/users`
- 使用HTTP状态码表示请求结果
- 统一的返回格式：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {}
  }
  ```
- 分页参数统一使用：pageNum, pageSize
- 版本控制：在URL中使用版本号，如`/api/v1/users`

### 文档规范

- **API文档**：使用Swagger注解，确保每个接口都有完整的参数说明和返回值说明
- **项目文档**：使用Markdown格式，包括README.md、CONTRIBUTING.md等
- **代码注释**：关键业务逻辑必须添加注释，复杂算法需要详细说明

详细规范请参考 [开发规范文档](docs/development-guide.md)

## 贡献指南

我们非常欢迎您为WW项目贡献代码。以下是贡献流程：

1. Fork 本仓库
2. 创建您的特性分支 (`git checkout -b feature/amazing-feature`)
3. 遵循项目开发规范进行开发
4. 编写必要的测试用例
5. 提交您的更改 (`git commit -m 'feat: add some amazing feature'`)
6. 推送到分支 (`git push origin feature/amazing-feature`)
7. 打开一个 Pull Request

更多详情请参阅 [贡献指南](CONTRIBUTING.md)

## 版本历史

查看完整的 [版本历史](CHANGELOG.md)

## 许可证

本项目采用 MIT 许可证 - 详情请参阅 [LICENSE](LICENSE) 文件

## 演示环境

我们提供了在线演示环境，您可以通过以下地址访问：

- 管理后台：[https://demo.ww6933.com/admin](https://demo.ww6933.com/admin)  
  用户名：admin  
  密码：123456

- 前台商城：[https://demo.ww6933.com/mall](https://demo.ww6933.com/mall)

- 移动端H5：[https://demo.ww6933.com/h5](https://demo.ww6933.com/h5)

*注：演示环境仅供体验，定期重置数据，请勿存储重要信息*

## 常见问题

### Q1: 如何修改服务端口？
A1: 在各服务的`application.yml`文件中修改`server.port`属性，或通过Nacos配置中心统一管理。

### Q2: 如何添加新的微服务模块？
A2: 参考现有模块结构，创建新的Maven模块，并在`pom.xml`中添加必要的依赖。确保在新模块的`bootstrap.yml`中配置正确的服务名和Nacos地址。

### Q3: 如何进行服务间调用？
A3: 项目支持两种服务间调用方式：
- 使用OpenFeign进行声明式REST调用
- 使用gRPC进行高性能RPC调用

### Q4: 如何处理分布式事务？
A4: 项目使用Seata处理分布式事务，支持AT、TCC和Saga三种模式。详细使用方法请参考[分布式事务文档](docs/distributed-transaction.md)。

### Q5: 如何进行系统监控？
A5: 系统集成了Prometheus和Grafana进行监控，SkyWalking进行链路追踪。详细配置方法请参考[监控配置文档](docs/monitoring.md)。

## 联系方式

- 项目维护者：[WW团队](mailto:ww6933@example.com)
- 项目主页：[https://gitee.com/ww6933/ww-app](https://gitee.com/ww6933/ww-app)
- 技术讨论：[Discussions](https://gitee.com/ww6933/ww-app/issues)
- 问题反馈：[Issues](https://gitee.com/ww6933/ww-app/issues)

## 致谢

感谢所有为本项目做出贡献的开发者！
