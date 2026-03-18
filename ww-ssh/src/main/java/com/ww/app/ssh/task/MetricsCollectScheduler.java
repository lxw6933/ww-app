package com.ww.app.ssh.task;

import com.ww.app.ssh.service.MetricsSnapshotService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 主机指标快照定时刷新任务。
 * <p>
 * 仅刷新最近仍有页面在浏览的活跃维度，
 * 使前端读取缓存即可获得较新的服务器/JVM 指标。
 * </p>
 */
@Component
public class MetricsCollectScheduler {

    /**
     * 指标快照服务。
     */
    private final MetricsSnapshotService metricsSnapshotService;

    /**
     * 构造方法。
     *
     * @param metricsSnapshotService 指标快照服务
     */
    public MetricsCollectScheduler(MetricsSnapshotService metricsSnapshotService) {
        this.metricsSnapshotService = metricsSnapshotService;
    }

    /**
     * 定时刷新活跃维度的主机指标快照。
     * <p>
     * 固定延迟 3 秒，兼顾 JVM 视图采样颗粒度与后端 SSH 压力。
     * </p>
     */
    @Scheduled(initialDelay = 1000L, fixedDelay = 3000L)
    public void refreshActiveMetrics() {
        metricsSnapshotService.refreshActiveSnapshots();
    }
}
