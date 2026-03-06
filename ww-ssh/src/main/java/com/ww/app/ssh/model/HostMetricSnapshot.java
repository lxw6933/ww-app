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
}
