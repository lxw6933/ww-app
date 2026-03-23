package com.ww.app.ssh.model;

import lombok.Data;

/**
 * 主机实时指标快照。
 * <p>
 * 用于承载某个“环境 + 服务实例”对应主机的关键运行指标，
 * 包括 CPU 使用率、内存使用率、内存容量和负载等信息。
 * 前端可直接消费该对象进行卡片渲染与状态展示。
 * </p>
 */
@Data
public class HostMetricSnapshot {

    /**
     * 项目名称。
     */
    private String project;

    /**
     * 环境名称。
     */
    private String env;

    /**
     * 服务名称（可能包含实例后缀）。
     */
    private String service;

    /**
     * 目标类型。
     * <p>
     * 当前主要用于区分 Java 应用与 nginx 等非 JVM 日志目标，
     * 便于前端按能力控制交互入口。
     * </p>
     */
    private String targetType;

    /**
     * 主机地址。
     */
    private String host;

    /**
     * CPU 使用率，单位百分比（0-100）。
     */
    private Double cpuUsagePercent;

    /**
     * 内存使用率，单位百分比（0-100）。
     */
    private Double memoryUsagePercent;

    /**
     * 已使用内存（MB）。
     */
    private Long memoryUsedMb;

    /**
     * 总内存（MB）。
     */
    private Long memoryTotalMb;

    /**
     * 交换内存使用率，单位百分比（0-100）。
     */
    private Double swapUsagePercent;

    /**
     * 已使用交换内存（MB）。
     */
    private Long swapUsedMb;

    /**
     * 交换内存总量（MB）。
     */
    private Long swapTotalMb;

    /**
     * 磁盘使用率，单位百分比（0-100）。
     */
    private Double diskUsagePercent;

    /**
     * 已使用磁盘容量（MB）。
     */
    private Long diskUsedMb;

    /**
     * 磁盘总容量（MB）。
     */
    private Long diskTotalMb;

    /**
     * 1 分钟平均负载。
     */
    private Double load1m;

    /**
     * 5 分钟平均负载。
     */
    private Double load5m;

    /**
     * 15 分钟平均负载。
     */
    private Double load15m;

    /**
     * 指标状态：ok / error。
     */
    private String status;

    /**
     * 状态说明（失败场景记录错误原因）。
     */
    private String message;

    /**
     * 快照采集时间（毫秒时间戳）。
     */
    private Long updatedAt;

    /**
     * 是否已配置实例启停脚本。
     * <p>
     * true 表示前端可展示“启动/重启/停止”运维按钮。
     * </p>
     */
    private Boolean canManage;

    /**
     * 是否支持 JVM 监控。
     * <p>
     * nginx 等非 JVM 目标应返回 false，前端据此隐藏 JVM 入口。
     * </p>
     */
    private Boolean canMonitorJvm;

    /**
     * 是否已配置中间件后台入口。
     * <p>
     * true 表示前端可展示“中间件”按钮，并加载该实例关联的测试环境后台地址。
     * </p>
     */
    private Boolean canOpenMiddleware;

    /**
     * 已配置的中间件后台数量。
     * <p>
     * 仅统计启用状态的配置项，便于前端做提示展示。
     * </p>
     */
    private Integer middlewareCount;

    /**
     * 实例运行状态。
     * <p>
     * 可选值：
     * 1. running：运行中；<br>
     * 2. stopped：已停止；<br>
     * 3. unknown：状态未知；<br>
     * 4. unconfigured：未配置运维脚本。
     * </p>
     */
    private String instanceStatus;

    /**
     * 实例状态详情。
     * <p>
     * 通常来自运维脚本 {@code status} 输出的摘要。
     * </p>
     */
    private String instanceStatusDetail;

    /**
     * JVM 进程号。
     * <p>
     * 仅在成功识别到目标 Java 进程时返回。
     * </p>
     */
    private Long jvmPid;

    /**
     * JVM Eden 区使用率，单位百分比（0-100）。
     */
    private Double jvmEdenUsagePercent;

    /**
     * JVM Old 区使用率，单位百分比（0-100）。
     */
    private Double jvmOldUsagePercent;

    /**
     * JVM Meta（Metaspace）使用率，单位百分比（0-100）。
     */
    private Double jvmMetaUsagePercent;

    /**
     * JVM Eden 区已使用容量（MB）。
     */
    private Double jvmEdenUsedMb;

    /**
     * JVM Eden 区总容量（MB）。
     */
    private Double jvmEdenCapacityMb;

    /**
     * JVM Survivor 区已使用容量（MB，S0U + S1U）。
     */
    private Double jvmSurvivorUsedMb;

    /**
     * JVM Survivor 区总容量（MB，S0C + S1C）。
     */
    private Double jvmSurvivorCapacityMb;

    /**
     * JVM Old 区已使用容量（MB）。
     */
    private Double jvmOldUsedMb;

    /**
     * JVM Old 区总容量（MB）。
     */
    private Double jvmOldCapacityMb;

    /**
     * JVM Meta 区已使用容量（MB）。
     */
    private Double jvmMetaUsedMb;

    /**
     * JVM Meta 区总容量（MB）。
     */
    private Double jvmMetaCapacityMb;

    /**
     * JVM Compressed Class Space 已使用容量（MB）。
     */
    private Double jvmCompressedClassUsedMb;

    /**
     * JVM Compressed Class Space 总容量（MB）。
     */
    private Double jvmCompressedClassCapacityMb;

    /**
     * JVM Heap 已使用容量（MB，Eden + Survivor + Old）。
     */
    private Double jvmHeapUsedMb;

    /**
     * JVM Heap 总容量（MB，Eden + Survivor + Old）。
     */
    private Double jvmHeapCapacityMb;

    /**
     * JVM Heap 使用率，单位百分比（0-100）。
     */
    private Double jvmHeapUsagePercent;

    /**
     * JVM 年轻代 GC 次数（YGC）。
     */
    private Long jvmYoungGcCount;

    /**
     * JVM 年轻代 GC 总耗时（秒，YGCT）。
     */
    private Double jvmYoungGcTimeSeconds;

    /**
     * JVM Full GC 次数（FGC）。
     */
    private Long jvmFullGcCount;

    /**
     * JVM Full GC 总耗时（秒，FGCT）。
     */
    private Double jvmFullGcTimeSeconds;

    /**
     * JVM GC 累计总耗时（秒，GCT）。
     */
    private Double jvmTotalGcTimeSeconds;

    /**
     * JVM GC 采集状态。
     * <p>
     * 可选值：
     * 1. ok：采集成功；<br>
     * 2. no_pid：未识别到 Java 进程；<br>
     * 3. no_jstat：目标机缺少 jstat；<br>
     * 4. parse_error：结果解析失败；<br>
     * 5. error：采集异常；<br>
     * 6. unknown：未采集。
     * </p>
     */
    private String jvmGcStatus;

    /**
     * JVM GC 状态说明。
     */
    private String jvmGcMessage;
}
