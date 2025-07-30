package com.ww.app.web.config.sys;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 系统资源监控器
 * 定期收集系统资源使用情况，包括CPU、内存、磁盘等
 */
@Slf4j
public class SystemResourcesMonitor implements ApplicationListener<ApplicationReadyEvent>, DisposableBean {

    /**
     * 监控频率（秒）
     */
    @Value("${app.system.monitor.frequency:30}")
    private int monitorFrequency;

    /**
     * 是否开启详细监控
     */
    @Value("${app.system.monitor.detailed:false}")
    private boolean detailed;

    /**
     * CPU使用率警告阈值（百分比）
     */
    @Value("${app.system.monitor.cpu-warning-threshold:80}")
    private double cpuWarningThreshold;

    /**
     * 内存使用率警告阈值（百分比）
     */
    @Value("${app.system.monitor.memory-warning-threshold:80}")
    private double memoryWarningThreshold;

    /**
     * 调度执行器
     */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "system-resources-monitor");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * 是否正在运行标志
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 应用启动完成后开始监控
     */
    @Override
    public void onApplicationEvent(@NotNull ApplicationReadyEvent event) {
        startMonitoring();
    }

    /**
     * 开始系统资源监控
     */
    public void startMonitoring() {
        if (running.compareAndSet(false, true)) {
            log.info("系统资源监控已启动，监控频率: {}秒", monitorFrequency);
            scheduler.scheduleAtFixedRate(this::monitorSystemResources, 0, monitorFrequency, TimeUnit.SECONDS);
        }
    }

    /**
     * 停止系统资源监控
     */
    public void stopMonitoring() {
        if (running.compareAndSet(true, false)) {
            log.info("系统资源监控已停止");
            try {
                // 设置较短的超时时间
                scheduler.shutdown();
                if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                    log.warn("系统资源监控器关闭超时，强制关闭");
                    scheduler.shutdownNow();
                    if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                        log.error("系统资源监控器无法正常关闭");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                scheduler.shutdownNow();
            }
        }
    }

    /**
     * 监控系统资源
     */
    private void monitorSystemResources() {
        try {
            // 获取操作系统信息
            OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
            
            // CPU信息
            double systemCpuLoad = getSystemCpuLoad(operatingSystemMXBean);
            double processCpuLoad = getProcessCpuLoad(operatingSystemMXBean);
//            int availableProcessors = operatingSystemMXBean.getAvailableProcessors();
            
            // 内存信息
            long freeMemory = Runtime.getRuntime().freeMemory();
            long totalMemory = Runtime.getRuntime().totalMemory();
            long maxMemory = Runtime.getRuntime().maxMemory();
            long usedMemory = totalMemory - freeMemory;
            
            // 计算使用率
            double memoryUsagePercent = (double) usedMemory / totalMemory * 100;
            
            // 记录基本信息
            StringBuilder sb = new StringBuilder("系统资源使用情况 - ");
            sb.append("CPU: ").append(String.format("%.2f", processCpuLoad)).append("% (进程), ")
              .append(String.format("%.2f", systemCpuLoad)).append("% (系统), ")
              .append("内存: ").append(String.format("%.2f", memoryUsagePercent)).append("%, ")
              .append(formatSize(usedMemory)).append("/").append(formatSize(totalMemory))
              .append(" (最大: ").append(formatSize(maxMemory)).append(")");
            
            // 根据阈值决定日志级别
            if (systemCpuLoad > cpuWarningThreshold || memoryUsagePercent > memoryWarningThreshold) {
                log.warn(sb.toString());
            } else {
                log.info(sb.toString());
            }
            
            // 详细信息
            if (detailed) {
                logDetailedSystemInfo(operatingSystemMXBean);
            }
        } catch (Exception e) {
            log.error("监控系统资源时发生错误", e);
        }
    }

    /**
     * 获取系统CPU负载
     */
    private double getSystemCpuLoad(OperatingSystemMXBean operatingSystemMXBean) {
        try {
            Method method = operatingSystemMXBean.getClass().getDeclaredMethod("getSystemCpuLoad");
            method.setAccessible(true);
            Double systemCpuLoad = (Double) method.invoke(operatingSystemMXBean);
            return systemCpuLoad != null ? systemCpuLoad * 100 : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 获取进程CPU负载
     */
    private double getProcessCpuLoad(OperatingSystemMXBean operatingSystemMXBean) {
        try {
            Method method = operatingSystemMXBean.getClass().getDeclaredMethod("getProcessCpuLoad");
            method.setAccessible(true);
            Double processCpuLoad = (Double) method.invoke(operatingSystemMXBean);
            return processCpuLoad != null ? processCpuLoad * 100 : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 记录详细系统信息
     */
    private void logDetailedSystemInfo(OperatingSystemMXBean operatingSystemMXBean) {
        log.info("===== 详细系统资源信息 =====");
        log.info("操作系统: {} {}", operatingSystemMXBean.getName(), operatingSystemMXBean.getVersion());
        log.info("操作系统架构: {}", operatingSystemMXBean.getArch());
        log.info("可用处理器数: {}", operatingSystemMXBean.getAvailableProcessors());
        log.info("系统负载平均值: {}", operatingSystemMXBean.getSystemLoadAverage());
        
        // 尝试获取更多系统信息（如果支持）
        try {
            Method getFreePhysicalMemorySize = operatingSystemMXBean.getClass().getDeclaredMethod("getFreePhysicalMemorySize");
            Method getTotalPhysicalMemorySize = operatingSystemMXBean.getClass().getDeclaredMethod("getTotalPhysicalMemorySize");
            Method getFreeSwapSpaceSize = operatingSystemMXBean.getClass().getDeclaredMethod("getFreeSwapSpaceSize");
            Method getTotalSwapSpaceSize = operatingSystemMXBean.getClass().getDeclaredMethod("getTotalSwapSpaceSize");
            
            getFreePhysicalMemorySize.setAccessible(true);
            getTotalPhysicalMemorySize.setAccessible(true);
            getFreeSwapSpaceSize.setAccessible(true);
            getTotalSwapSpaceSize.setAccessible(true);
            
            long freePhysicalMemorySize = (Long) getFreePhysicalMemorySize.invoke(operatingSystemMXBean);
            long totalPhysicalMemorySize = (Long) getTotalPhysicalMemorySize.invoke(operatingSystemMXBean);
            long freeSwapSpaceSize = (Long) getFreeSwapSpaceSize.invoke(operatingSystemMXBean);
            long totalSwapSpaceSize = (Long) getTotalSwapSpaceSize.invoke(operatingSystemMXBean);
            
            log.info("物理内存: 空闲 {} / 总计 {}", 
                    formatSize(freePhysicalMemorySize), formatSize(totalPhysicalMemorySize));
            log.info("交换空间: 空闲 {} / 总计 {}", 
                    formatSize(freeSwapSpaceSize), formatSize(totalSwapSpaceSize));
        } catch (Exception e) {
            log.debug("无法获取详细系统内存信息", e);
        }
        
        log.info("JVM内存: 已用 {} / 已分配 {} / 最大 {}", 
                formatSize(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()),
                formatSize(Runtime.getRuntime().totalMemory()),
                formatSize(Runtime.getRuntime().maxMemory()));
        
        log.info("===============================");
    }

    /**
     * 格式化字节大小为人类可读格式
     */
    private String formatSize(long bytes) {
        double kilobytes = bytes / 1024.0;
        if (kilobytes < 1024) {
            return String.format("%.2f KB", kilobytes);
        }
        
        double megabytes = kilobytes / 1024.0;
        if (megabytes < 1024) {
            return String.format("%.2f MB", megabytes);
        }
        
        double gigabytes = megabytes / 1024.0;
        return String.format("%.2f GB", gigabytes);
    }

    /**
     * 应用关闭时停止监控
     */
    @Override
    public void destroy() {
        stopMonitoring();
    }
} 