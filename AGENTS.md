# 仓库指南

## 项目结构与模块组织
这是一个用于 Spring Cloud 微服务的多模块 Maven 单体仓库。顶层模块包括 `ww-api-gateway`、`ww-auth`、`ww-mall`、`ww-im`、`ww-framework` 等。每个服务或子模块遵循标准 Maven 布局：代码在 `src/main/java`，配置/资源在 `src/main/resources`。测试在 `src/test/java`。数据库脚本位于 `script/sql`。

## 构建、测试与开发命令
- `mvn clean package -DskipTests`: 构建所有模块但不运行测试。
- `mvn clean package -pl ww-api-gateway -am -DskipTests`: 构建单个模块及其依赖。
- `mvn test`: 运行整个仓库的测试。
- `mvn -pl ww-api-gateway test`: 运行单个模块的测试。
- `java -jar ww-api-gateway/target/ww-api-gateway.jar`: 运行打包后的网关服务。

## 编码风格与命名规范
- Java：遵循阿里巴巴 Java 编码规范（类名 UpperCamelCase，方法/变量 lowerCamelCase，常量 UPPER_SNAKE_CASE）。
- 类、字段、方法需要 Javadoc。
- 新增的类与方法必须添加详细的中文注释（Javadoc 或行内说明），说明用途、关键参数、边界条件、异常或副作用。
- 前端（如在 UI 模块中）：Vue 组件使用 PascalCase，CSS 使用 BEM，JS 使用 camelCase。
- 该仓库未统一配置格式化/代码检查工具；保持与相邻文件一致。

## 测试指南
- 测试使用 Spring Boot 的 `spring-boot-starter-test`（基于 JUnit）。测试放在 `src/test/java`。
- 测试命名优先使用 `*Test` 或 `*Tests`，并靠近对应模块。
- 没有明确的覆盖率阈值；新逻辑应保持有意义的覆盖。

## 提交与 PR 指南
- 此环境中无法访问 Git 历史；提交信息遵循 Angular 风格：
  `<type>(<scope>): <subject>`，可选 body/footer（例如：`feat(ww-auth): add token refresh`）。
- PR 需包含：摘要、测试说明（运行的命令）、UI 变更截图。

## 配置与安全提示
- 服务配置通常在 `application.yml`/`bootstrap.yml` 以及 Nacos 中。
- 不要提交密钥；使用本地覆盖或环境变量。
- 数据库初始化在 `script/sql`；schema 变更需版本化并记录。

## 并发与分布式系统规则

- 任何涉及以下内容的代码：
  - Redis
  - MQ 消费/生产
  - 定时任务
  - 分布式锁
  - 重试机制

  必须明确考虑：
  - 幂等性
  - 重复执行
  - 失败与重试场景
  - 集群/多实例行为

- 避免全局锁；按业务键粒度加锁
- 消费者必须具备幂等性设计
- 假设至少一次投递语义

## 交互与方案评估规则
- 对你提出的方案，我会先进行可行性与风险评估，不会无脑顺从。
- 若方案存在不合理、风险过高或与仓库规范冲突之处，我会明确指出并给出替代建议。
- 在给出最终代码前，我会完成必要的代码走读与自检，尽量避免语法错误、逻辑错误、以及无效或冗余代码。

## 交付前自检清单
- 语法与编译：确认代码语法正确、类型/导入/依赖完整。
- 逻辑与边界：核心逻辑可达，边界条件、空值与异常路径被处理。
- 一致性：命名、风格、注释与周边代码一致，新增类/方法含详细中文注释。
- 冗余清理：移除无效代码、未使用变量/导入、重复逻辑。
- 影响评估：涉及配置、数据结构或接口变更时，确认兼容性与潜在影响。
