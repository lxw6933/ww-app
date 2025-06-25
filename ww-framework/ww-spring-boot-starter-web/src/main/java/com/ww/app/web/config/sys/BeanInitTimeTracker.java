package com.ww.app.web.config.sys;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Bean初始化时间监控器
 * 跟踪所有Bean的初始化时间，识别耗时较长的Bean，辅助优化应用启动性能
 */
@Slf4j
@Order(Integer.MIN_VALUE) // 确保这个处理器最先执行
public class BeanInitTimeTracker implements BeanPostProcessor, ApplicationListener<ContextRefreshedEvent> {

    /**
     * 耗时统计的阈值，超过此时间(ms)的Bean初始化会被记录和警告
     */
    @Value("${app.bean.init.warning-threshold:50}")
    private long warningThreshold;

    /**
     * 是否记录所有Bean的初始化时间，即使没有超过阈值
     */
    @Value("${app.bean.init.track-all:false}")
    private boolean trackAll;

    /**
     * 是否在应用启动后输出详细的初始化时间统计报告
     */
    @Value("${app.bean.init.report-enabled:true}")
    private boolean reportEnabled;

    /**
     * 报告中显示的最慢Bean数量
     */
    @Value("${app.bean.init.top-slow-count:20}")
    private int topSlowCount;

    /**
     * 存储Bean初始化开始时间
     */
    private final Map<String, Long> startTimes = new ConcurrentHashMap<>();

    /**
     * 存储Bean的初始化耗时信息
     */
    private final Map<String, BeanInitInfo> beanInitInfoMap = new ConcurrentHashMap<>();

    /**
     * 总计监控的Bean数量
     */
    private int totalTrackedBeans = 0;

    /**
     * 总计超过阈值的Bean数量
     */
    private int totalSlowBeans = 0;

    /**
     * 初始化开始前的回调
     */
    @Override
    public Object postProcessBeforeInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
        startTimes.put(beanName, System.currentTimeMillis());
        return bean;
    }

    /**
     * 初始化完成后的回调
     */
    @Override
    public Object postProcessAfterInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
        Long startTime = startTimes.remove(beanName);
        if (startTime != null) {
            long cost = System.currentTimeMillis() - startTime;
            totalTrackedBeans++;
            
            // 记录初始化耗时
            if (cost > warningThreshold) {
                totalSlowBeans++;
                log.warn("Bean[{}]初始化耗时较长: {} ms", beanName, cost);
                recordBeanInitInfo(beanName, bean.getClass().getName(), cost);
            } else if (trackAll) {
                log.debug("Bean[{}]初始化耗时: {} ms", beanName, cost);
                recordBeanInitInfo(beanName, bean.getClass().getName(), cost);
            }
        }
        return bean;
    }

    /**
     * 记录Bean初始化信息
     */
    private void recordBeanInitInfo(String beanName, String className, long initTime) {
        BeanInitInfo info = new BeanInitInfo();
        info.setBeanName(beanName);
        info.setClassName(className);
        info.setInitTime(initTime);
        beanInitInfoMap.put(beanName, info);
    }

    /**
     * 应用上下文刷新完成后的回调，用于生成报告
     */
    @Override
    public void onApplicationEvent(@NotNull ContextRefreshedEvent event) {
        if (reportEnabled) {
            generateReport();
        }
    }

    /**
     * 生成Bean初始化时间统计报告
     */
    private void generateReport() {
        if (beanInitInfoMap.isEmpty()) {
            log.info("没有收集到Bean初始化时间信息");
            return;
        }

        log.info("===== Bean初始化时间统计报告 =====");
        log.info("总计监控Bean数量: {}", totalTrackedBeans);
        log.info("初始化耗时超过{}ms的Bean数量: {}", warningThreshold, totalSlowBeans);

        // 计算平均初始化时间
        double avgTime = beanInitInfoMap.values().stream()
                .mapToLong(BeanInitInfo::getInitTime)
                .average()
                .orElse(0);
        log.info("Bean平均初始化时间: {} ms", String.format("%.2f", avgTime));

        // 获取最慢的N个Bean
        List<BeanInitInfo> slowestBeans = beanInitInfoMap.values().stream()
                .sorted(Comparator.comparingLong(BeanInitInfo::getInitTime).reversed())
                .limit(topSlowCount)
                .collect(Collectors.toList());

        // 输出最慢Bean的详细信息
        log.info("===== 初始化最慢的{}个Bean =====", slowestBeans.size());
        for (int i = 0; i < slowestBeans.size(); i++) {
            BeanInitInfo info = slowestBeans.get(i);
            log.info("{}. Bean[{}] - {} ms ({})", 
                    i + 1, info.getBeanName(), info.getInitTime(), info.getClassName());
        }

        // 按类型分组统计
        Map<String, List<BeanInitInfo>> groupByType = beanInitInfoMap.values().stream()
                .collect(Collectors.groupingBy(info -> {
                    String className = info.getClassName();
                    // 提取包名部分
                    int lastDot = className.lastIndexOf('.');
                    if (lastDot > 0) {
                        return className.substring(0, lastDot);
                    }
                    return className;
                }));

        // 计算每个包的总初始化时间
        log.info("===== 按包名统计Bean初始化时间 =====");
        groupByType.entrySet().stream()
                .sorted(Map.Entry.<String, List<BeanInitInfo>>comparingByValue(
                        Comparator.comparingLong(list -> 
                                list.stream().mapToLong(BeanInitInfo::getInitTime).sum())
                ).reversed())
                .limit(10)  // 只显示前10个耗时最长的包
                .forEach(entry -> {
                    long totalTime = entry.getValue().stream()
                            .mapToLong(BeanInitInfo::getInitTime)
                            .sum();
                    log.info("包[{}] - 总耗时: {} ms, Bean数量: {}", 
                            entry.getKey(), totalTime, entry.getValue().size());
                });

        log.info("=====================================");
    }

    /**
     * Bean初始化信息数据类
     */
    @Data
    private static class BeanInitInfo {
        /**
         * Bean名称
         */
        private String beanName;
        
        /**
         * Bean类名
         */
        private String className;
        
        /**
         * 初始化耗时(ms)
         */
        private long initTime;
    }
}
