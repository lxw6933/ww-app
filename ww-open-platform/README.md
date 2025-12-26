# 开放平台服务 (ww-open-platform)

## 项目概述

ww-open-platform 是一个生产级别的开放平台服务，为第三方应用提供安全、高性能、可扩展的API接入能力。该平台实现了完整的应用管理、API管理、权限控制、安全认证、监控统计等功能。

## 核心功能

### 1. 应用管理模块

- **应用注册**：支持第三方应用注册，自动生成应用密钥
- **应用审核**：支持应用审核流程，包括待审核、已启用、已禁用、已拒绝等状态
- **应用信息管理**：支持应用信息查询、更新、启用/禁用等操作
- **IP白名单**：支持配置IP白名单，限制应用访问来源
- **调用限额**：支持配置每日和每分钟调用限额

**核心实体**：
- `OpenApplication`：应用信息实体
- `BusinessClientInfo`：商户信息实体（关联应用）

### 2. API管理模块

- **API注册**：支持API接口注册，包括路径、方法、版本等信息
- **API版本管理**：支持API版本控制，便于API升级和兼容
- **API状态管理**：支持开发中、已发布、已下线、已废弃等状态
- **API文档**：支持API文档自动生成和查询

**核心实体**：
- `OpenApiInfo`：API信息实体

### 3. 权限管理模块

- **权限授权**：支持为应用授权访问特定API
- **权限撤销**：支持撤销应用的API访问权限
- **权限验证**：自动验证应用是否有权限调用API
- **自定义限流**：支持为应用配置自定义的API限流策略

**核心实体**：
- `OpenApiPermission`：API权限实体

### 4. 安全认证模块

#### 4.1 签名验证
- 使用RSA数字签名算法验证请求合法性
- 签名规则：`sysCode + appCode + methodCode + transId + data`的JSON字符串
- 支持Base64编码的公钥验证

**密钥架构说明：**
- **商户级别（BusinessClientInfo）**：
  - `publicKey`（公钥）：保存在系统中，用于验证商户的签名
  - `privateKey`（私钥）：由商户保管，用于对API请求进行数字签名
  - 商户入驻时，如果未提供密钥对，系统会自动生成2048位RSA密钥对
  - 一个商户可以拥有多个应用，所有应用共享商户的RSA密钥对
  
- **应用级别（OpenApplication）**：
  - `appSecret`（应用密钥）：当前是UUID字符串，用于应用身份标识
  - **注意**：`appSecret` 目前不参与签名验证，签名验证使用的是商户级别的RSA密钥对
  - 未来可用于其他验证场景（如HMAC签名、Token生成等）

**业务流程：**
1. 商户入驻：创建 `BusinessClientInfo`，自动生成或使用商户提供的RSA密钥对
2. 密钥分发：将私钥安全地分发给商户（建议通过安全渠道，如加密邮件、密钥管理系统等）
3. 应用注册：商户使用 `sysCode` 注册应用，系统自动生成 `appSecret`
4. 签名验证：客户端使用商户私钥签名，服务端使用商户公钥验签

#### 4.2 防重放攻击
- 基于流水号（transId）的防重放机制
- 使用Redis缓存已处理的请求，有效期5分钟
- 自动拒绝重复请求

#### 4.3 请求时间验证
- 验证请求时间与服务器时间的偏差
- 允许最大时间偏差：300秒（5分钟）
- 防止过期请求被重放

#### 4.4 IP白名单验证
- 支持配置IP白名单
- 自动验证请求来源IP是否在白名单中
- 支持多个IP，用逗号分隔

#### 4.5 限流控制
- 支持QPS（每秒查询数）限流
- 支持应用级别的限流配置
- 支持API级别的限流配置
- 基于Redis的分布式限流实现

**核心组件**：
- `OpenApiInterceptor`：安全拦截器，实现所有安全验证逻辑

### 5. 监控统计模块

#### 5.1 调用日志
- 记录所有API调用日志
- 包含请求参数、响应结果、耗时、状态等信息
- 支持异步记录，提高性能
- 自动脱敏处理敏感信息

#### 5.2 统计信息
- 按日统计API调用次数、成功率、平均响应时间等
- 支持按应用、按API维度统计
- 自动计算最大、最小、平均响应时间

**核心实体**：
- `OpenApiCallLog`：API调用日志实体
- `OpenApiStatistics`：API统计实体

### 6. 配置管理模块

- **动态配置**：支持动态配置系统参数
- **配置缓存**：使用Redis缓存配置，提高查询性能
- **配置分组**：支持配置分组管理
- **环境隔离**：支持不同环境的配置隔离

**核心实体**：
- `OpenConfig`：配置实体

## 技术架构

### 技术栈

- **框架**：Spring Boot 2.7.18
- **ORM**：MyBatis-Plus
- **缓存**：Redis（Redisson）
- **数据库**：MySQL
- **消息队列**：RabbitMQ（可选）
- **安全**：RSA数字签名

### 核心组件

1. **OpenApiInterceptor**：安全拦截器
   - 实现签名验证、限流、防重放等安全功能
   - 自动验证应用和API信息
   - 设置请求上下文

2. **OpenApiLogAspect**：日志切面
   - 自动记录API调用日志
   - 异步保存日志，不影响性能
   - 自动更新统计信息

3. **OpenApiContext**：请求上下文
   - 存储当前请求的上下文信息
   - 使用ThreadLocal实现线程隔离
   - 自动清理，防止内存泄漏

## 使用指南

### 1. 应用注册流程

```java
// 1. 创建应用
OpenApplication application = new OpenApplication();
application.setAppCode("APP001");
application.setAppName("示例应用");
application.setSysCode("SYS001");
application.setDescription("这是一个示例应用");

// 2. 注册应用（自动生成密钥）
openApplicationService.registerApplication(application);

// 3. 审核应用
openApplicationService.auditApplication("APP001", 
    ApplicationStatus.ENABLED.getCode(), 
    "审核通过", 
    "admin");
```

### 2. API注册流程

```java
// 1. 创建API信息
OpenApiInfo apiInfo = new OpenApiInfo();
apiInfo.setApiCode("user.info");
apiInfo.setApiName("获取用户信息");
apiInfo.setApiPath("/open/api/user/info");
apiInfo.setHttpMethod("POST");
apiInfo.setDescription("获取用户详细信息");
apiInfo.setDefaultQps(100L); // 默认限流：100 QPS

// 2. 注册API
openApiInfoService.registerApi(apiInfo);

// 3. 发布API
openApiInfoService.publishApi("user.info");
```

### 3. 权限授权流程

```java
// 1. 创建权限
OpenApiPermission permission = new OpenApiPermission();
permission.setAppCode("APP001");
permission.setApiCode("user.info");
permission.setStatus(1); // 启用
permission.setCustomQps(200L); // 自定义限流：200 QPS

// 2. 授权
openApiPermissionService.grantPermission(permission);
```

### 4. 客户端调用示例

```java
// 1. 构建请求
BaseOpenRequest<Map<String, Object>> request = new BaseOpenRequest<>();
request.setTransId(IdUtil.nextIdStr());
request.setSysCode("SYS001");
request.setAppCode("APP001");
request.setMethodCode("user.info");
request.setReqTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));

// 2. 构建业务数据
Map<String, Object> data = new HashMap<>();
data.put("userId", "123456");
request.setData(data);

// 3. 生成签名
String reqData = request.getSysCode() + request.getAppCode() + 
                request.getMethodCode() + request.getTransId() + 
                JSON.toJSONString(request.getData());
String sign = DigitalSignatureUtil.generationSignature(
    reqData, 
    Base64.decodeBase64(privateKey)
);
request.setSign(sign);

// 4. 发送请求
// 使用HTTP客户端发送POST请求到 /open/api/user/info
```

## 安全特性

### 1. 多层安全防护

- **签名验证**：确保请求来源合法
- **防重放攻击**：防止请求被重复使用
- **时间验证**：防止过期请求
- **IP白名单**：限制访问来源
- **限流控制**：防止接口被滥用

### 2. 数据安全

- **敏感信息脱敏**：日志中自动脱敏敏感信息
- **加密传输**：建议使用HTTPS传输
- **密钥管理**：应用密钥安全存储

### 3. 性能优化

- **缓存机制**：应用信息、API信息、权限信息等使用Redis缓存
- **异步处理**：日志和统计信息异步处理，不影响接口性能
- **连接池**：数据库连接池优化

## 扩展性设计

### 1. 插件化架构

- 支持自定义拦截器
- 支持自定义验证逻辑
- 支持自定义限流策略

### 2. 多租户支持

- 基于商户编码（sysCode）的多租户隔离
- 支持不同商户的独立配置

### 3. 动态配置

- 支持动态修改配置，无需重启
- 配置变更自动刷新缓存

## 监控与运维

### 1. 日志监控

- 所有API调用都有详细日志记录
- 支持日志查询和分析
- 支持错误日志告警

### 2. 性能监控

- 自动统计API响应时间
- 支持性能分析和优化
- 支持慢查询识别

### 3. 告警机制

- 支持调用失败率告警
- 支持响应时间告警
- 支持限流触发告警

## 数据库设计

### 核心表结构

1. **open_application**：应用信息表
2. **open_api_info**：API信息表
3. **open_api_permission**：API权限表
4. **open_api_call_log**：API调用日志表
5. **open_api_statistics**：API统计表
6. **open_config**：配置表
7. **open_business_client_info**：商户信息表

## 最佳实践

### 1. 应用管理

- 定期审核应用状态
- 及时禁用异常应用
- 合理配置调用限额

### 2. API设计

- 遵循RESTful规范
- 版本化管理API
- 提供清晰的API文档

### 3. 安全实践

- 定期更换应用密钥
- 配置合理的IP白名单
- 设置合适的限流策略

### 4. 性能优化

- 合理使用缓存
- 优化数据库查询
- 异步处理非关键操作

## 注意事项

1. **签名算法**：确保客户端和服务端使用相同的签名算法
2. **时间同步**：确保客户端和服务端时间同步
3. **密钥安全**：妥善保管应用密钥，不要泄露
4. **限流配置**：根据实际业务需求合理配置限流参数
5. **日志存储**：定期清理历史日志，避免存储空间不足

## 后续优化方向

1. **OAuth2.0支持**：支持OAuth2.0认证方式
2. **API网关集成**：与API网关深度集成
3. **监控大屏**：提供可视化的监控大屏
4. **自动化测试**：提供API自动化测试工具
5. **SDK支持**：提供多语言SDK支持

## 联系方式

如有问题或建议，请联系开发团队。


