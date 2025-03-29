<p align="center">
 <img src="https://img.shields.io/badge/Spring%20Cloud-2021-blue.svg" alt="Spring Cloud">
 <img src="https://img.shields.io/badge/Spring%20Boot-2.7.18-blue.svg" alt="Spring Boot">
 <img src="https://img.shields.io/badge/Java-1.8-blue.svg" alt="Java">
 <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License">
</p>

# WW App 微服务架构

## 项目概述

WW App 是一个基于 Spring Cloud 和 Spring Boot 的分布式微服务架构项目，旨在提供高性能、可扩展的企业级应用开发框架。该项目采用模块化设计，支持多种功能模块的快速集成，包括即时通讯、电子商务、认证授权等功能。

## 软件架构

项目采用微服务架构，主要包含以下模块：

| 模块 | 说明 | 主要功能 |
|-----|------|--------|
| ww-framework | 基础框架 | 提供各种 Spring Boot Starter，支持各种中间件集成 |
| ww-dependencies | 依赖管理 | 统一管理项目依赖版本，确保各模块依赖一致性 |
| ww-consumer | 消费者服务 | 处理用户请求和业务逻辑 |
| ww-flink | 实时数据处理 | 基于 Apache Flink 的流式数据处理服务 |
| ww-auth | 认证授权服务 | 提供统一的用户认证和授权功能 |
| ww-third-server | 第三方服务 | 对接各种第三方服务，如支付、短信等 |
| ww-api-gateway | API 网关 | 统一接口入口，提供路由、过滤、认证等功能 |
| ww-admin-manage | 后台管理 | 提供系统管理和配置功能 |
| ww-grpc | gRPC 服务 | 提供高性能的 RPC 调用功能 |
| ww-open-platform | 开发平台 | 提供开放能力，支持第三方应用接入 |
| ww-im | 即时通讯 | 提供实时消息传递和聊天功能 |
| ww-mall | 电子商务 | 提供完整的电商业务解决方案 |

## 模块详情

### ww-framework

提供项目的基础框架和通用功能，包含多个 Spring Boot Starter：

| 子模块 | 说明 |
|-------|------|
| ww-common | 通用工具类和基础功能 |
| ww-spring-boot-starter-web | Web 应用支持 |
| ww-spring-boot-starter-monitor | 应用监控功能 |
| ww-spring-boot-starter-es | Elasticsearch 集成 |
| ww-spring-boot-starter-excel | Excel 文件处理 |
| ww-spring-boot-starter-influxdb | InfluxDB 时序数据库集成 |
| ww-spring-boot-starter-ip | IP 地址处理 |
| ww-spring-boot-starter-minio | MinIO 对象存储 |
| ww-spring-boot-starter-mongodb | MongoDB 文档数据库集成 |
| ww-spring-boot-starter-mybatis | MyBatis ORM 框架集成 |
| ww-spring-boot-starter-rabbitmq | RabbitMQ 消息队列集成 |
| ww-spring-boot-starter-kafka | Kafka 消息队列集成 |
| ww-spring-boot-starter-redis | Redis 缓存集成 |
| ww-spring-boot-starter-security | 安全认证和授权 |
| ww-spring-boot-starter-sensitive | 敏感信息处理 |
| ww-spring-boot-starter-websocket | WebSocket 实时通信支持 |
| ww-spring-boot-starter-job | 任务调度功能 |

### ww-mall

电子商务模块，提供完整的电商业务功能：

| 子模块 | 说明 |
|-------|------|
| ww-cart | 购物车服务 |
| ww-coupon | 优惠券服务 |
| ww-order | 订单服务 |
| ww-pay | 支付服务 |
| ww-member | 会员服务 |
| ww-search | 搜索服务 |
| ww-product | 商品服务 |
| ww-seckill | 秒杀服务 |

### ww-im

即时通讯模块，提供实时消息传递和聊天功能：

| 子模块 | 说明 |
|-------|------|
| ww-im-api | IM 服务 API |
| ww-im-biz | IM 业务实现 |
| ww-im-router-api | 消息路由 API |
| ww-im-router-biz | 消息路由实现 |
| ww-im-core-api | IM 核心 API |
| ww-im-core-biz | IM 核心实现 |
| ww-im-redpacket-api | 红包功能 API |
| ww-im-redpacket-biz | 红包功能实现 |

### ww-grpc

gRPC 服务模块，提供高性能的远程过程调用：

| 子模块 | 说明 |
|-------|------|
| ww-grpc-api | gRPC 服务接口定义 |
| ww-grpc-server | gRPC 服务端实现 |
| ww-grpc-client | gRPC 客户端实现 |

### ww-admin-manage

后台管理模块，提供系统管理和配置功能：

| 子模块 | 说明 |
|-------|------|
| ww-admin-manage-api | 管理接口定义 |
| ww-admin-manage-biz | 管理功能实现 |

### ww-third-server

第三方服务模块，提供与外部服务的集成：

| 子模块 | 说明 |
|-------|------|
| ww-third-server-api | 第三方服务接口定义 |
| ww-third-server-biz | 第三方服务实现 |

## 技术栈

| 技术 | 说明 | 版本 |
|-----|------|------|
| Spring Boot | 应用开发框架 | 2.7.18 |
| Spring Cloud | 微服务框架 | 2021.x |
| MyBatis | ORM 框架 | 最新版 |
| Redis | 缓存数据库 | 最新版 |
| MongoDB | NoSQL 数据库 | 最新版 |
| Elasticsearch | 搜索引擎 | 最新版 |
| RabbitMQ | 消息队列 | 最新版 |
| Kafka | 消息队列 | 最新版 |
| MinIO | 对象存储 | 最新版 |
| gRPC | 远程过程调用 | 最新版 |
| WebSocket | 实时通信 | 最新版 |
| Apache Flink | 流处理框架 | 最新版 |

## 环境要求

* JDK 1.8+
* Maven 3.6+
* MySQL 5.7+
* Redis 5.0+
* 其他中间件根据需要配置

## 快速开始

### 安装教程

1. 克隆项目到本地
   ```bash
   git clone https://github.com/your-username/ww-app.git
   ```

2. 进入项目目录
   ```bash
   cd ww-app
   ```

3. 编译安装
   ```bash
   mvn clean install -DskipTests
   ```

### 启动服务

1. 启动 Nacos 注册中心和配置中心

2. 启动 Redis、MySQL 等基础服务

3. 启动网关服务
   ```bash
   cd ww-api-gateway
   mvn spring-boot:run
   ```

4. 启动其他业务服务
   ```bash
   cd ww-auth
   mvn spring-boot:run
   ```

## 功能特性

- [x] 分布式微服务架构
- [x] 统一认证授权
- [x] API 网关集成
- [x] 服务注册与发现
- [x] 配置中心
- [x] 分布式事务
- [x] 消息队列集成
- [x] 即时通讯功能
- [x] 电商系统功能
- [x] 第三方服务集成
- [x] 高性能 RPC 调用
- [x] 实时数据处理

## 项目规范

### 开发规范

* 统一的代码风格和格式
* 完善的注释和文档
* 单元测试覆盖
* 分支管理和版本控制

### 命名规范

* 包名：com.ww.app.{模块名}.{功能名}
* 类名：驼峰式命名，首字母大写
* 方法名：驼峰式命名，首字母小写
* 变量名：驼峰式命名，首字母小写

## 参与贡献
1. Fork 本仓库
2. 新建分支：`git checkout -b feature/your-feature`
3. 提交代码：`git commit -m 'Add some feature'`
4. 推送到远程分支：`git push origin feature/your-feature`
5. 提交 Pull Request

## 许可证
本项目使用 MIT 许可证，详情请参阅 LICENSE 文件。

## 联系方式
如有任何问题或建议，请通过 [ww6933mail@gmail.com](mailto:ww6933mail@gmail.com) 联系我们。