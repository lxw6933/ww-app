### SkyWalking 使用指南

本文档面向 Java 应用，指导如何使用 SkyWalking Agent 接入、开启全链路 TraceId 日志、优化 Agent 启动、按需过滤不必要的链路，并提供生产建议与常见问题排查。

参考：
- OAP 后端概览（Tracing/Metrics/Logging 能力）：[Backend Overview](https://skywalking.apache.org/docs/main/v9.7.0/en/concepts-and-designs/backend-overview/)
- OAL 规则编写示例：[OAL](https://skywalking.apache.org/docs/main/v9.7.0/en/concepts-and-designs/oal/)

## 一、接入 SkyWalking Agent（必读）

1) 下载 Agent 包（与 OAP 版本大致匹配），解压后记下 `skywalking-agent.jar` 的绝对路径。

2) 启动参数（最小必需项）：

Windows（PowerShell）示例：
```powershell
$env:SW_AGENT_NAME="ww-api-gateway"
$env:SW_AGENT_COLLECTOR_BACKEND_SERVICES="172.28.174.84:11800"
java -javaagent:D:\ww-project\skywalking-agent\skywalking-agent.jar -jar app.jar
```

Linux/macOS 示例：
```bash
export SW_AGENT_NAME=ww-api-gateway
export SW_AGENT_COLLECTOR_BACKEND_SERVICES=172.28.174.84:11800
java -javaagent:/opt/skywalking-agent/skywalking-agent.jar -jar app.jar
```

- `SW_AGENT_COLLECTOR_BACKEND_SERVICES`：OAP 的 gRPC 地址（默认端口 11800）。
- `SW_AGENT_NAME`：上报到 OAP 的服务名，务必与实际服务一一对应。

可选常用参数（按需）：
- `SW_AGENT_INSTANCE_NAME`：实例名（不配置则默认主机名+PID）。
- 更多参数见 `agent/config/agent.config`，按照注释进行开启/调整。

## 二、开启 TraceId 全链路日志

1) 引入依赖（Logback 1.x 示例）：
```xml
<dependency>
    <groupId>org.apache.skywalking</groupId>
    <artifactId>apm-toolkit-logback-1.x</artifactId>
    <version>${skywalking.version}</version>
</dependency>
```

2) 在 Logback 中配置日志格式与 SkyWalking GRPC Appender（8.4.0+ 支持）：
```xml
<!-- 文件输出格式：不包含颜色代码，保持纯文本 -->
<property name="PATTERN_FILE" value="%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd HH:mm:ss.SSS}} | %5p ${PID:- } | %thread [%tid] %-40.40logger{39} | %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}"/>

<!-- 通过 SkyWalking GRPC 上报日志到 OAP（日志与 Trace 自动关联） -->
<appender name="GRPC" class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.log.GRPCLogClientAppender">
    <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
        <layout class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.TraceIdPatternLogbackLayout">
            <pattern>${PATTERN_FILE}</pattern>
        </layout>
    </encoder>
</appender>

<!-- 示例：将 GRPC Appender 绑定到 root（可与 FILE 同时输出） -->
<root level="INFO">
    <appender-ref ref="GRPC"/>
    <!-- <appender-ref ref="FILE"/> -->
    <!-- 按需添加你的 FILE/CONSOLE appender -->
    
</root>
```

3) 线程池场景 TraceId 传播
- 建议开启与线程池上下文传播相关的 Agent 插件（通常位于 `optional-plugins` 或 `bootstrap-plugins`，如与“traceable thread pool”相关的插件），并将其复制到 `plugins` 目录以生效。具体插件名称以所用 Agent 版本为准。

## 三、优化 Agent 启动与运行

为缩短启动时间、降低运行开销，建议仅保留必要插件：

1) 打开 Agent 的 `plugins` 目录，移动项目不需要的插件 JAR 到备份目录（或 `optional-plugins`）。示例（若未使用）：
- baidu-brpc 相关
- cassandra-java-driver 相关
- canal 相关
- jetty 相关
- clickhouse 相关
- 未使用的 MQ 相关插件（仅保留实际用到的）

2) 调整采样等高级配置：
- 开发/联调可临时提高采样率以便排查；生产建议保持默认或按量级评估，避免采样过高导致存储与网络压力增大。
- 更多高级项（插件开关、忽略规则等）请阅读 `agent/config/agent.config` 注释。

## 四、按需过滤上报到 OAP 的请求

在 Agent 的 `config` 目录新建 `apm-trace-ignore-plugin.config` 文件：
```properties
# 忽略路径数据上报（支持方法:路径 的形式；多个规则用英文逗号分隔）
# 匹配语法：
#   /path/?   单个字符
#   /path/*   多个字符
#   /path/**  多个字符和多级路径
trace.ignore_path=${SW_AGENT_TRACE_IGNORE_PATH:GET:/*/actuator/**,GET:/swagger-ui/**,GET:/v3/api-docs/**,GET:/swagger-resources/**,GET:/favicon.ico,GET:/error}
```

说明：
- 可通过环境变量覆盖：`SW_AGENT_TRACE_IGNORE_PATH`。
- 建议过滤健康检查、文档、静态资源等非业务链路，降低 OAP 压力。

## 五、常见问题排查（FAQ）

1) 无法连接 OAP
- 检查 `SW_AGENT_COLLECTOR_BACKEND_SERVICES` 地址、端口（11800）是否可达、防火墙是否放行。
- 若开启 TLS，请确保 Agent 与 OAP 的 TLS 设置一致。

2) 服务名异常或聚合混乱
- 确认 `SW_AGENT_NAME` 与实际服务一一对应；避免多服务使用相同名称。

3) 看不到 Trace 或日志未入库
- 检查采样策略与忽略规则是否过于激进。
- 确认日志配置已将 `GRPC` appender 绑定到 logger（root 或具体 logger）。

4) 线程池中 TraceId 丢失
- 确认线程/线程池上下文传播相关插件已放入 `plugins` 并生效。

## 六、生产指标与告警（示例）


