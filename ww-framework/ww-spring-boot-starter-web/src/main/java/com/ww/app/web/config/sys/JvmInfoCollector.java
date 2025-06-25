package com.ww.app.web.config.sys;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.lang.management.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JVM信息收集器
 * 在应用启动完成后收集并记录JVM相关信息，帮助分析性能问题
 */
@Slf4j
public class JvmInfoCollector implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * 应用启动完成后收集JVM信息
     */
    @Override
    public void onApplicationEvent(@NotNull ApplicationReadyEvent event) {
        logJvmInfo();
    }

    /**
     * 收集并记录JVM信息
     */
    public void logJvmInfo() {
        log.info("===== JVM信息 =====");
        
        // JVM基本信息
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        log.info("JVM名称: {}", runtimeMXBean.getVmName());
        log.info("JVM版本: {}", runtimeMXBean.getVmVersion());
        log.info("JVM供应商: {}", runtimeMXBean.getVmVendor());
        log.info("JVM启动参数: {}", runtimeMXBean.getInputArguments());
        log.info("JVM启动时间: {} ms", runtimeMXBean.getStartTime());
        log.info("JVM运行时间: {} ms", runtimeMXBean.getUptime());

        // 内存信息
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
        
        log.info("堆内存初始大小: {} MB", bytesToMB(heapMemoryUsage.getInit()));
        log.info("堆内存使用量: {} MB", bytesToMB(heapMemoryUsage.getUsed()));
        log.info("堆内存当前分配: {} MB", bytesToMB(heapMemoryUsage.getCommitted()));
        log.info("堆内存最大限制: {} MB", bytesToMB(heapMemoryUsage.getMax()));
        
        log.info("非堆内存初始大小: {} MB", bytesToMB(nonHeapMemoryUsage.getInit()));
        log.info("非堆内存使用量: {} MB", bytesToMB(nonHeapMemoryUsage.getUsed()));
        log.info("非堆内存当前分配: {} MB", bytesToMB(nonHeapMemoryUsage.getCommitted()));
        log.info("非堆内存最大限制: {} MB", bytesToMB(nonHeapMemoryUsage.getMax()));

        // 线程信息
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        log.info("当前活动线程数: {}", threadMXBean.getThreadCount());
        log.info("峰值线程数: {}", threadMXBean.getPeakThreadCount());
        log.info("已启动线程总数: {}", threadMXBean.getTotalStartedThreadCount());
        log.info("守护线程数: {}", threadMXBean.getDaemonThreadCount());

        // GC信息
        List<GarbageCollectorMXBean> garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean garbageCollectorMXBean : garbageCollectorMXBeans) {
            log.info("GC名称: {}", garbageCollectorMXBean.getName());
            log.info("GC收集次数: {}", garbageCollectorMXBean.getCollectionCount());
            log.info("GC收集总时间: {} ms", garbageCollectorMXBean.getCollectionTime());
        }

        // 类加载信息
        ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
        log.info("当前加载类数量: {}", classLoadingMXBean.getLoadedClassCount());
        log.info("已加载类总数: {}", classLoadingMXBean.getTotalLoadedClassCount());
        log.info("已卸载类总数: {}", classLoadingMXBean.getUnloadedClassCount());

        // 系统属性
        Map<String, String> systemProperties = new HashMap<>();
        System.getProperties().forEach((k, v) -> systemProperties.put(k.toString(), v.toString()));
        
        log.info("Java版本: {}", systemProperties.get("java.version"));
        log.info("Java供应商: {}", systemProperties.get("java.vendor"));
        log.info("Java主目录: {}", systemProperties.get("java.home"));
        log.info("操作系统名称: {}", systemProperties.get("os.name"));
        log.info("操作系统版本: {}", systemProperties.get("os.version"));
        log.info("操作系统架构: {}", systemProperties.get("os.arch"));
        log.info("用户名: {}", systemProperties.get("user.name"));
        log.info("用户主目录: {}", systemProperties.get("user.home"));
        log.info("工作目录: {}", systemProperties.get("user.dir"));
        
        log.info("========================");
    }

    /**
     * 字节转MB
     */
    private double bytesToMB(long bytes) {
        return bytes / (1024.0 * 1024.0);
    }
} 