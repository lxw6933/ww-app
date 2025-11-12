# 🧠 AI IDE 全局规则：Java 企业开发标准

## 1️⃣ 用户背景
用户是一名拥有10年以上经验的资深 Java 架构师，主要使用：
- Spring Boot 2.7+
- Redis（含 Redisson、BloomFilter）
- MongoDB（集群架构、读写分离）
- 高并发、异步批量处理（Disruptor、线程池、队列）
- 分布式环境与多模块微服务

用户提的需求往往是系统级、性能导向、工程化要求高。  
请不要生成“入门级”或“简化版”示例。

---

## 2️⃣ 生成风格要求

### 🏗️ 通用要求
- 所有代码必须满足**健壮性**、**线程安全**、**可扩展**、**易维护**原则。
- 在涉及多线程、缓存、持久化时，必须考虑：
    - 原子性：确保操作的不可分割性，尤其是在分布式环境中。
    - 一致性：确保数据在所有节点上的一致性，特别是在高并发场景下。
    - 异常回滚与日志记录：所有关键操作必须有异常处理机制，并记录详细的日志信息。

### ✅ 注释规范

**类注释必须包含**:
```java
/**
 * @author ww
 * @create YYYY-MM-DD HH:mm
 * @description: 类的功能描述
 */
```

**方法注释必须包含**:
- 方法功能描述
- @param 参数说明（如果有参数）
- @return 返回值说明
- 复杂业务逻辑添加行内注释

**复杂业务逻辑示例**:
```java
/**
 * 处理用户登录请求
 * 
 * @param userId 用户ID，不能为空
 * @param password 用户密码，不能为空
 * @return 登录结果，成功返回true，失败返回false
 */
public boolean handleLogin(Long userId, String password) {
    // 校验输入参数
    if (userId == null || password == null) {
        throw new IllegalArgumentException("用户ID和密码不能为空");
    }
    
    // 业务逻辑实现
    try {
        // 调用认证服务
        return authService.authenticate(userId, password);
    } catch (Exception e) {
        log.error("用户登录失败: {}", e.getMessage(), e);
        throw new ApiException(ErrorCodeConstants.LOGIN_FAILED);
    }
}
```

### ✅ 技术栈最佳实践

#### Redis
- 使用 `RedissonComponent` 提供的高性能封装方法，避免重复实现。
- 示例代码：
```java
@Resource
private RedissonComponent redissonComponent;

public void setKeyValue(String key, String value) {
    Map<String, String> keyValueMap = new HashMap<>();
    keyValueMap.put(key, value);
    redissonComponent.batchSet(keyValueMap);
}

public String getValue(String key) {
    Map<String, String> result = redissonComponent.batchGet(Collections.singletonList(key));
    return result.get(key);
}
```

#### MongoDB
- 使用 `MongoTemplate` 进行数据操作，确保事务支持。
- 推荐使用 `ww-spring-boot-starter-mongodb` 提供的工具类（如存在）。
- 示例代码：
```java
@Resource
private MongoTemplate mongoTemplate;

public void saveUser(User user) {
    mongoTemplate.save(user);
}

public User findUserById(String id) {
    return mongoTemplate.findById(id, User.class);
}
```

#### RabbitMQ
- 使用 `RabbitTemplate` 发送消息，确保消息的可靠传递。
- 推荐使用 `ww-spring-boot-starter-rabbitmq` 提供的工具类（如存在）。
- 示例代码：
```java
@Resource
private RabbitTemplate rabbitTemplate;

public void sendMessage(String exchange, String routingKey, Object message) {
    rabbitTemplate.convertAndSend(exchange, routingKey, message);
}
```



- 在涉及多线程、缓存、持久化时，必须考虑：
    - 原子性
    - 一致性（如分布式场景）
    - 异常回滚与日志记录
- 若涉及持久化，请考虑批量入库优化与延迟写策略。
- 遇到 Redis、MongoDB、MySQL 等操作时，优先使用对应starter模块下的代码：
    - 连接池、Template、Redisson、事务支持。
- 若需求中未指定异常处理，默认添加：
    - 日志记录 (`log.error(...)`)
    - 统一异常包装 (`ServiceException`)

---

## 3️⃣ 输出格式要求
- 生成完整的可运行类结构（含 import、package、类、方法体）
- 若涉及多个职责，请自动分层拆分（service、config、listener、component）
- 添加 Javadoc 注释，说明输入输出与线程安全性
- 使用标准命名规范（驼峰、清晰语义）
- 若可优化，请主动在代码后提供“可选优化建议”

### ✅ 代码风格
- 每行代码长度不超过 120 个字符
- 类成员变量和方法之间保留一个空行
- 方法之间保留一个空行
- 导入语句按标准库、第三方库、项目内部的顺序组织

### ✅ 测试和验证
- 单元测试：每个公共方法都应有对应的单元测试。
- 集成测试：确保各个模块之间的交互正常。
- 测试覆盖率：目标覆盖率应达到 80% 以上。

示例单元测试：
```java
@Test
public void testHandleLogin() {
    // 准备测试数据
    Long userId = 1L;
    String password = "password123";
    
    // 调用方法
    boolean result = userService.handleLogin(userId, password);
    
    // 验证结果
    assertTrue(result);
}
```


---

## 4️⃣ 特定规则

### ✅ 项目基本信息

- **项目名称**: WW App 微服务系统
- **技术栈**: Spring Boot 2.7.18, Java 8, Spring Cloud Alibaba
- **构建工具**: Maven
- **包基础路径**: com.ww.app
- **服务注册**: Nacos

### ✅ 注释规范

**类注释必须包含**:
```java
/**
 * @author ww
 * @create YYYY-MM-DD- HH:mm
 * @description: 类的功能描述
 */
```

**方法注释必须包含**:
- 方法功能描述
- @param 参数说明（如果有参数）
- @return 返回值说明
- 复杂业务逻辑添加行内注释

### ✅ 代码格式

- 每行代码长度不超过 120 个字符
- 类成员变量和方法之间保留一个空行
- 方法之间保留一个空行
- 导入语句按标准库、第三方库、项目内部的顺序组织

### ✅ Spring Boot
- 默认使用注解方式配置 Bean，不写 XML。
- 如果有配置类，使用 `@Configuration` + `@Bean`。
- 若需要异步，请使用 `@Async` 或线程池（指定拒绝策略与核心数）。

### ✅ 全局异常处理

项目已配置全局异常处理器 `ResExceptionHandler`，会自动处理 `ApiException` 并返回统一的 `Result<T>` 格式。

### ✅ Redis
- 优先使用ww-spring-boot-redis-starter模块下的组件代码

### ✅ MongoDB
- 优先使用ww-spring-boot-mongodb-starter模块下的组件代码

### ✅ 并发 & 队列
- Disruptor优先使用ww-spring-boot-disruptor-starter模块下的组件代码
- 若涉及队列，默认采用：
    - 高性能结构：`ConcurrentLinkedQueue` / Disruptor
    - 有界队列 + 异步消费线程池
- 若涉及批量入库或消费，默认实现“批量获取 + 重试机制 + 日志补偿”

### ✅ 日志与监控
- 所有任务、线程、异步操作必须带关键日志，推荐使用 `@Slf4j` 注解。
- 若涉及性能关键路径，请添加耗时监控或计数器，优先使用 `Micrometer` 或 `SkyWalking`。
- 示例代码：
```java
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PerformanceService {

    private final MeterRegistry meterRegistry;

    public PerformanceService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void performTask() {
        long startTime = System.currentTimeMillis();
        try {
            // 业务逻辑
            log.info("任务开始执行");
            // 模拟任务
            Thread.sleep(1000);
        } catch (Exception e) {
            log.error("任务执行失败: {}", e.getMessage(), e);
            throw new ApiException(ErrorCodeConstants.TASK_FAILED);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            meterRegistry.counter("task.duration").increment(duration);
            log.info("任务执行完成，耗时: {} ms", duration);
        }
    }
}
```


---

## 5️⃣ 响应逻辑理解
- 当用户说“写一个功能”、“写一个工具类”、“改造为高并发版本”，
  理解为需要生产级、线程安全、可监控、可扩展实现。
- 当用户说“写个demo”，若无特别说明，也应生成简洁但健壮的可运行版本。
- 当需求模糊时，请先输出【你的理解 + 实现方案】再写代码。

---

## 6️⃣ 输出语言
- 默认使用中文解释、英文代码。
- 注释与 Javadoc 使用中文（简洁明了）。

## 重要提醒

1. **严格按照项目规范** 生成代码，必须先参考ww-framework包下所有的模块，里面有抽取好的很多方法，如果发现里面的方法可以优化或者有问题，提出你的优化和修改建议
2. **参考现有代码** 的写法，保持代码风格一致
3. **异常处理** 必须使用 ApiException，不要使用其他异常类
4. **错误码** 必须在 ErrorCodeConstants 中定义
5. **用户信息** 必须通过 AuthorizationContext 获取
6. **依赖注入** 必须使用 @Resource，不要使用 @Autowired
7. **日志记录** 必须使用 @Slf4j，错误日志必须包含异常堆栈
8. **参数校验** 必须在 Controller 和 Service 层都进行
9. **代码注释** 必须完整，包含类注释和方法注释
10. **Swagger 文档** 必须为所有 API 接口添加完整的文档注解
